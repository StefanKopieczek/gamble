package com.kopieczek.gamble.hardware.graphics;

import com.kopieczek.gamble.hardware.cpu.Interrupt;
import com.kopieczek.gamble.hardware.memory.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

public class Gpu {
    private static final Logger log = LogManager.getLogger(Gpu.class);
    public static final int DISPLAY_WIDTH = 160;
    public static final int DISPLAY_HEIGHT= 144;
    public static final int VIRTUAL_TOTAL_HEIGHT = 153; // Including VBlank
    private final Memory memory;
    private final Io io;
    private final InterruptLine interrupts;
    private final GraphicsAccessController graphicsAccessController;
    private final SpriteMap spriteMap;
    private final Color[][] screenBuffer = initScreenBuffer();
    private final Color[][] scratchBuffer = initScreenBuffer();
    private Mode mode = Mode.OAM_READ;
    private int modeClock = 0;
    private int currentLine = 0;

    public Gpu(Memory memory, Io io, InterruptLine interrupts, GraphicsAccessController graphicsAccessController,
               Oam oam, Vram vram) {
        this.memory = memory;
        this.io = io;
        this.interrupts = interrupts;
        this.graphicsAccessController = graphicsAccessController;
        this.spriteMap = new SpriteMap(oam, vram);
    }

    private static Color[][] initScreenBuffer() {
        Color[][] buffer = new Color[DISPLAY_HEIGHT][];
        for (int rowIdx = 0; rowIdx < DISPLAY_HEIGHT; rowIdx++) {
            buffer[rowIdx] = new Color[DISPLAY_WIDTH];
            for (int colIdx = 0; colIdx < DISPLAY_WIDTH; colIdx++) {
                buffer[rowIdx][colIdx] = Color.BLACK;
            }
        }

        return buffer;
    }

    public Color[][] getScreenBuffer() {
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
                    io.setHBlankInterrupt(true);
                    interrupts.setInterrupt(Interrupt.LCD_STAT);
                    break;
                case HBLANK:
                    currentLine++;
                    if (currentLine == DISPLAY_HEIGHT - 1) {
                        interrupts.setInterrupt(Interrupt.V_BLANK);
                        io.setVBlankInterrupt(true);
                        interrupts.setInterrupt(Interrupt.LCD_STAT);
                        flushBuffer();
                        changeMode(Mode.VBLANK);
                    } else {
                        changeMode(Mode.OAM_READ);
                        graphicsAccessController.setOamAccessible(false);
                        io.setOamInterrupt(true);
                        interrupts.setInterrupt(Interrupt.LCD_STAT);
                    }
                    break;
                case VBLANK:
                    currentLine = 0;
                    changeMode(Mode.OAM_READ);
                    graphicsAccessController.setOamAccessible(false);
                    io.setOamInterrupt(true);
                    interrupts.setInterrupt(Interrupt.LCD_STAT);
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
        for (int y = 0; y < scratchBuffer.length; y++) {
            for (int x = 0; x < scratchBuffer[0].length; x++) {
                scratchBuffer[y][x] = Color.WHITE;
            }
        }

        flushBuffer();
        modeClock = 0;
        currentLine = 0;
        mode = Mode.OAM_READ;
    }

    private void flushBuffer() {
        synchronized (screenBuffer){
            for (int y = 0; y < screenBuffer.length; y++) {
                for (int x = 0; x < screenBuffer[0].length; x++) {
                    screenBuffer[y][x] = scratchBuffer[y][x];
                }
            }
        }
    }

    private void renderLine(int currentLine) {
        final int tileY = ((currentLine + io.getScrollY()) / 8) % 32;
        for (int currentColumn = 0; currentColumn < DISPLAY_WIDTH; currentColumn++) {
            int tileX = ((currentColumn + io.getScrollX()) / 8) % 32;
            int tileMapIdx = 32 * tileY + tileX;
            int tileDataStart = getTileDataAddress(tileMapIdx);
            int[] rowData = getRowData(tileDataStart, (currentLine + io.getScrollY()) % 8);
            Color[] rowPixels = extractPixels(rowData);
            scratchBuffer[currentLine][currentColumn] = rowPixels[currentColumn % 8];
        }
    }

    private static Color[] extractPixels(int[] rowData) {
        Color[] pixels = new Color[8];
        for (int idx = 0; idx < 8; idx++) {
            int mask = 0x80 >> idx;
            boolean lowBit = (rowData[0] & mask) > 0;
            boolean highBit = (rowData[1] & mask) > 0;
            pixels[idx] = decodeColor(highBit, lowBit);
        }

        return pixels;
    }

    private static Color decodeColor(boolean highBit, boolean lowBit) {
        if (highBit && lowBit) {
            return Color.BLACK;
        } else if (highBit) {
            return new Color(192, 192, 192);
        } else if (lowBit) {
            return new Color(96, 96, 96);
        } else {
            return Color.WHITE;
        }
    }

    private int[] getRowData(int tileDataStart, int rowIdx) {
        int rowDataStart = tileDataStart + rowIdx * 2;
        return new int[] {memory.readByte(rowDataStart), memory.readByte(rowDataStart + 1)};
    }

    private int getTileDataAddress(int tileMapIdx) {
        int tileDataIdx = memory.readByte(io.getBackgroundTileMapStartAddress() + tileMapIdx);
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

    int getSpriteDataAddress(int index) {
        int spriteHeight = io.getSpriteHeight();
        int maxIndex = (spriteHeight == 8) ? 256 : 128;
        if ((0 <= index) && (index < maxIndex)) {
            return 0x8000 + index * 2 * io.getSpriteHeight();
        } else {
            throw new IllegalArgumentException("0x" + Integer.toHexString(index));
        }
    }

    public int[] getSpriteData(int index) {
        int address = getSpriteDataAddress(index);
        int numBytes = 2 * io.getSpriteHeight();
        int[] result = new int[numBytes];
        for (int i = 0; i < numBytes; i++) {
           result[i] = memory.readByte(address);
           address++;
        }

        return result;
    }

    public int getSpritePatternAddress(int spriteIndex) {
        if ((0 <= spriteIndex) && (spriteIndex < 40)) {
            return 0xfe00 + 4 * spriteIndex;
        } else {
            throw new IllegalArgumentException(Integer.toHexString(spriteIndex));
        }
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
