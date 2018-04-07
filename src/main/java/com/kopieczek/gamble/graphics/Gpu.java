package com.kopieczek.gamble.graphics;

import com.kopieczek.gamble.memory.MemoryManagementUnit;

import java.awt.*;

public class Gpu {
    public static final int DISPLAY_WIDTH = 160;
    public static final int DISPLAY_HEIGHT= 144;
    public static final int VIRTUAL_TOTAL_HEIGHT = 153; // Including VBlank
    private final MemoryManagementUnit mmu;
    private final Color[][] screenBuffer = initScreenBuffer();
    private Mode mode = Mode.OAM_READ;
    private int modeClock = 0;
    private int currentLine = 0;

    public Gpu(MemoryManagementUnit mmu) {
        this.mmu = mmu;
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
                    break;
                case HBLANK:
                    currentLine++;
                    if (currentLine == DISPLAY_HEIGHT - 1) {
                        // TODO: Fire V_BLANK interrupt
                        changeMode(Mode.VBLANK);
                    } else {
                        changeMode(Mode.OAM_READ);
                    }
                    break;
                case VBLANK:
                    currentLine = 0;
                    changeMode(Mode.OAM_READ);
                    break;
                default:
                    throw new IllegalStateException("Unknown GPU mode " + mode);
            }
        } else if (mode == Mode.VBLANK) {
            float progress = ((float)modeClock) / mode.duration;
            currentLine = (int)(DISPLAY_HEIGHT + progress * (VIRTUAL_TOTAL_HEIGHT - DISPLAY_HEIGHT));
        }

        mmu.setByte(0xff44, currentLine);
    }

    private void renderLine(int currentLine) {
        final int tileY = currentLine / 8;
        synchronized (screenBuffer) {
            for (int tileX = 0; tileX < DISPLAY_WIDTH / 8; tileX++) {
                int tileMapIdx = 32 * tileY + tileX;
                int tileDataStart = getTileDataAddress(tileMapIdx);
                int[] rowData = getRowData(tileDataStart, currentLine % 8);
                Color[] rowPixels = extractPixels(rowData);
                for (int subX = 0; subX < 8; subX++) {
                    screenBuffer[currentLine][tileX * 8 + subX] = rowPixels[subX];
                }
            }
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
        return new int[] {mmu.readByte(rowDataStart), mmu.readByte(rowDataStart + 1)};
    }

    private TileSet getTileSet() {
        if ((mmu.readByte(0xff40) & 0x08) == 0) {
            return TileSet.SECONDARY; //TileSet.PRIMARY; -- TODO remove this hack --
        } else {
            return TileSet.SECONDARY;
        }
    }

    private int getTileDataAddress(int tileMapIdx) {
        TileSet tileSet = getTileSet();
        int tileDataIdx = mmu.readByte(0x9800 + tileMapIdx); // TODO should use tileset rather than hard coding
        if (tileSet.isDataIndexSigned && tileDataIdx > 128) {
            tileDataIdx -= 256;
        }

        return tileSet.dataZeroAddress + tileDataIdx * 16;
    }

    private void changeMode(Mode newMode) {
        modeClock %= mode.duration;
        mode = newMode;
    }

    private enum Mode {
        OAM_READ(80),
        VRAM_READ(172),
        HBLANK(204),
        VBLANK(456 * DISPLAY_HEIGHT);

        public final int duration;

        Mode(int ticks) {
            duration = ticks;
        }
    }

    private enum TileSet {
        PRIMARY(0x9800, 0x9000, true),
        SECONDARY(0x9c00, 0x8000, false);

        public final int mapStartAddress;
        public final int dataZeroAddress;
        public final boolean isDataIndexSigned;

        TileSet(int mapStartAddress, int dataZeroAddress, boolean isDataIndexSigned) {
            this.mapStartAddress = mapStartAddress;
            this.dataZeroAddress = dataZeroAddress;
            this.isDataIndexSigned = isDataIndexSigned;
        }
    }
}
