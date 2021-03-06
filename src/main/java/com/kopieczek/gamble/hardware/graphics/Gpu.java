package com.kopieczek.gamble.hardware.graphics;

import com.google.common.collect.Lists;
import com.kopieczek.gamble.hardware.cpu.Interrupt;
import com.kopieczek.gamble.hardware.memory.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.Arrays;

public class Gpu {
    private static final Logger log = LogManager.getLogger(Gpu.class);
    public static final int DISPLAY_WIDTH = 160;
    public static final int DISPLAY_HEIGHT= 144;
    public static final int VIRTUAL_TOTAL_HEIGHT = 153; // Including VBlank
    private static final boolean DEBUG_MODE = false;
    private final Memory memory;
    private final Io io;
    private final InterruptLine interrupts;
    private final GraphicsAccessController graphicsAccessController;
    private final SpriteMap spriteMap;
    private final ScreenBuffer screenBuffer = new ScreenBuffer(DISPLAY_WIDTH, DISPLAY_HEIGHT);
    private Mode mode = Mode.OAM_READ;
    private int modeClock = 0;
    private int currentLine = 0;

    public Gpu(Memory memory, Io io, InterruptLine interrupts, GraphicsAccessController graphicsAccessController,
               Oam oam, Vram vram) {
        this.memory = memory;
        this.io = io;
        this.interrupts = interrupts;
        this.graphicsAccessController = graphicsAccessController;
        this.spriteMap = new SpriteMap(io, oam, vram);
        this.spriteMap.init();
    }

    public ScreenBuffer getScreenBuffer() {
        return screenBuffer;
    }

    public void stepAhead(int cycles) {
        modeClock += cycles;

        if (modeClock >= mode.duration) {
            switch (mode) {
                case OAM_READ:
                    changeMode(Mode.VRAM_READ);
                    break;
                case VRAM_READ:
                    renderLine(currentLine);
                    changeMode(Mode.HBLANK);
                    graphicsAccessController.setVramAccessible(true);
                    graphicsAccessController.setOamAccessible(true);
                    io.handleHBlank();
                    break;
                case HBLANK:
                    currentLine++;
                    if (currentLine == DISPLAY_HEIGHT - 1) {
                        interrupts.setInterrupt(Interrupt.V_BLANK);
                        io.handleVBlank();
                        screenBuffer.swapScratchBuffers();
                        changeMode(Mode.VBLANK);
                    } else {
                        changeMode(Mode.OAM_READ);
                        graphicsAccessController.setOamAccessible(false);
                        io.handleOam();
                    }
                    break;
                case VBLANK:
                    currentLine = 0;
                    changeMode(Mode.OAM_READ);
                    graphicsAccessController.setOamAccessible(false);
                    io.handleOam();
                    break;
                default:
                    throw new IllegalStateException("Unknown GPU mode " + mode);
            }
        } else if (mode == Mode.VBLANK) {
            currentLine = DISPLAY_HEIGHT + modeClock / 456;
        }

        io.setLcdCurrentLine(currentLine);
        io.setLcdControllerMode(mode.toLcdMode());
    }

    public void stop() {
        Color[][] scratch = screenBuffer.getScratch();
        for (int y = 0; y < scratch.length; y++) {
            for (int x = 0; x < scratch[0].length; x++) {
                scratch[y][x] = Color.WHITE;
            }
        }

        screenBuffer.swapScratchBuffers();
        modeClock = 0;
        currentLine = 0;
        mode = Mode.OAM_READ;
    }

    private void renderLine(int currentLine) {
        java.util.List<Sprite> sprites = getAllSprites(currentLine);
        clearLine(currentLine);
        renderBackgroundSprites(sprites, currentLine);
        renderTiles(currentLine);
        renderWindowTiles(currentLine);
        renderForegroundSprites(sprites, currentLine);
        if (DEBUG_MODE) {
            renderGrid();
        }
    }

    private void renderWindowTiles(int currentLine) {
        if (io.isWindowDisplayEnabled() &&
                -7 <= io.getWindowX() && io.getWindowX() <= 159 &&
                0 <= io.getWindowY() && io.getWindowY() <= 143) {
            if (currentLine < io.getWindowY()) {
                return;
            }

            final int tileY = (currentLine - io.getWindowY()) / 8;

            Color[][] scratch = screenBuffer.getScratch();
            for (int currentColumn = Math.max(io.getWindowX(), 0); currentColumn < DISPLAY_WIDTH; currentColumn++) {
                int x = currentColumn - io.getWindowX();

                int tileX = x / 8;
                int tileMapIdx = 32 * tileY + tileX;
                int tileDataStart = getWindowTileDataAddress(tileMapIdx);
                int[] rowData = getRowData(tileDataStart, (currentLine - io.getWindowY()) % 8);
                int[] rowColors = extractColors(rowData);
                int currentColor = rowColors[x % 8];
                scratch[currentLine][currentColumn] = decodeColor(currentColor);
            }
        }
    }

    private void clearLine(int currentLine) {
        // Null out line so the background can spot which pixels have background sprites.
        Color[][] scratch = screenBuffer.getScratch();
        for (int col = 0; col < DISPLAY_WIDTH; col++) {
            scratch[currentLine][col] = null;
        }
    }

    private void renderTiles(int currentLine) {
        Color[][] scratch = screenBuffer.getScratch();
        final int tileY = ((currentLine + io.getScrollY()) / 8) % 32;
        for (int currentColumn = 0; currentColumn < DISPLAY_WIDTH; currentColumn++) {
            int x = currentColumn + io.getScrollX();
            int tileX = (x / 8) % 32;
            int tileMapIdx = 32 * tileY + tileX;
            int tileDataStart = getBackgroundTileDataAddress(tileMapIdx);
            int[] rowData = getRowData(tileDataStart, (currentLine + io.getScrollY()) % 8);
            int[] rowColors = extractColors(rowData);
            int currentColor = rowColors[x % 8];
            if (currentColor > 0 || (scratch[currentLine][currentColumn] == null)) {
                scratch[currentLine][currentColumn] = decodeColor(currentColor);
            }
        }
    }

    private java.util.List<Sprite> getAllSprites(int currentLine) {
        java.util.List<Sprite> sprites = spriteMap.getSpritesForRow(currentLine);
        sprites = sprites.subList(0, Math.min(10, sprites.size())); // Only 10 sprites can be displayed on each line
        return Lists.reverse(sprites);  // Blit sprites in reverse priority order
    }

    private void renderBackgroundSprites(java.util.List<Sprite> sprites, int currentLine) {
        sprites.stream()
               .filter(sprite -> sprite.getAttributes().getZPosition().equals(SpriteAttributes.ZPosition.BACKGROUND))
               .forEach(sprite -> renderSpriteRow(sprite, currentLine));
    }

    private void renderForegroundSprites(java.util.List<Sprite> sprites, int currentLine) {
        sprites.stream()
                .filter(sprite -> sprite.getAttributes().getZPosition().equals(SpriteAttributes.ZPosition.FOREGROUND))
                .forEach(sprite -> renderSpriteRow(sprite, currentLine));
    }

    private void renderSpriteRow(Sprite sprite, int currentLine) {
        int spriteX = sprite.getAttributes().getX();
        int spriteY = sprite.getAttributes().getY();
        int rowOffset = currentLine - spriteY;
        Color[] rowPixels = Arrays.copyOf(sprite.getPixels()[rowOffset], sprite.getPixels()[rowOffset].length);
        if (DEBUG_MODE) {
            for (int idx = 0; idx < rowPixels.length; idx++) {
                Color old = rowPixels[idx];
                if (rowOffset < 8) {
                    rowPixels[idx] = new Color(255, old.getGreen() / 2, old.getBlue() / 2);
                } else if (rowOffset < 16){
                    rowPixels[idx] = new Color(old.getRed() / 2, old.getGreen() / 2, 255);
                } else {
                    throw new IllegalArgumentException("Row offset " + rowOffset);
                }
            }
        }

        Color[][] scratch = screenBuffer.getScratch();
        for (int xOffset = 0; xOffset < rowPixels.length; xOffset++) {
            int x = spriteX + xOffset;
            if (0 <= x && x < DISPLAY_WIDTH && rowPixels[xOffset].getAlpha() > 0) {
                scratch[currentLine][x] = rowPixels[xOffset];
            }
        }
    }

    private void renderGrid() {
        Color[][] scratch = screenBuffer.getScratch();
        for (int rowIdx = 0; rowIdx < DISPLAY_HEIGHT; rowIdx++) {
            for (int colIdx = 0; colIdx < DISPLAY_WIDTH; colIdx++) {
                if ((rowIdx % 8) * (colIdx % 8) == 0) {
                    scratch[rowIdx][colIdx] = Color.RED;
                }
            }
        }
    }

    private static int[] extractColors(int[] rowData) {
        int[] colors = new int[8];
        for (int idx = 0; idx < 8; idx++) {
            int mask = 0x80 >> idx;
            boolean lowBit = (rowData[0] & mask) > 0;
            boolean highBit = (rowData[1] & mask) > 0;
            colors[idx] = (highBit ? 2 : 0) + (lowBit ? 1 : 0);
        }

        return colors;
    }

    private Color decodeColor(int colorId) {
        return io.getShadeForBackgroundColor(colorId);
    }

    private int[] getRowData(int tileDataStart, int rowIdx) {
        int rowDataStart = tileDataStart + rowIdx * 2;
        return new int[] {memory.readByte(rowDataStart), memory.readByte(rowDataStart + 1)};
    }

    private int getBackgroundTileDataAddress(int tileMapIdx) {
        return getTileDataAddress(io.getBackgroundTileMapStartAddress(), tileMapIdx);
    }

    private int getWindowTileDataAddress(int tileMapIdx) {
        return getTileDataAddress(io.getWindowTileMapStartAddress(), tileMapIdx);
    }

    private int getTileDataAddress(int mapStartAddress, int tileMapIndex) {
        int tileDataIdx = memory.readByte(mapStartAddress + tileMapIndex);
        if (io.areTileMapEntriesSigned()) {
            // Slightly magic.
            // If tile map entries are signed then 0x00 indicates tile 0, 0x80 indicates tile 128,
            // 0xff indicates tile -1, etc.
            // Since tile -127 is at the start of the tile data area, what we're doing in effect is treating 0x00 as
            // tile 128, 0x80 as tile 0, 0xff as tile -127, etc.
            tileDataIdx = (tileDataIdx + 128) % 256;
        }

        return io.getTileDataStartAddress() + tileDataIdx * 16;
    }

    private void changeMode(Mode newMode) {
        modeClock %= mode.duration;
        mode = newMode;
    }

    private enum Mode {
        OAM_READ(80),
        VRAM_READ(172),
        HBLANK(204),
        VBLANK(4560);

        public final int duration;

        Mode(int ticks) {
            duration = ticks;
        }

        Io.LcdControllerMode toLcdMode() {
            switch(this) {
                case OAM_READ:
                    return Io.LcdControllerMode.OAM_READ;
                case VRAM_READ:
                    return Io.LcdControllerMode.DATA_TRANSFER;
                case HBLANK:
                    return Io.LcdControllerMode.HBLANK;
                case VBLANK:
                    return Io.LcdControllerMode.VBLANK;
                default:
                    throw new IllegalArgumentException("Unknown Gpu mode " + this);
            }
        }
    }
}
