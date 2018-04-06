package com.kopieczek.gamble.graphics;

import com.kopieczek.gamble.memory.MemoryManagementUnit;

import java.awt.*;

public class Gpu {
    public static final int DISPLAY_WIDTH = 160;
    public static final int DISPLAY_HEIGHT= 144;
    private final MemoryManagementUnit mmu;
    private final Color[][] screenBuffer = initScreenBuffer();

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
}
