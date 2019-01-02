package com.kopieczek.gamble.hardware.graphics;

import java.awt.*;

public class ScreenBuffer {
    private Color[][] screen;
    private Color[][] scratch;
    private Color[][] passive;
    private final Object lock = new Object();

    public ScreenBuffer(int width, int height) {
        screen = new Color[height][];
        scratch = new Color[height][];
        passive = new Color[height][];
        for (int rowIdx = 0; rowIdx < height; rowIdx++) {
            screen[rowIdx] = new Color[width];
            scratch[rowIdx] = new Color[width];
            passive[rowIdx] = new Color[width];
            for (int colIdx = 0; colIdx < width; colIdx++) {
                screen[rowIdx][colIdx] = Color.BLACK;
                scratch[rowIdx][colIdx] = Color.BLACK;
                passive[rowIdx][colIdx] = Color.BLACK;
            }
        }
        screen = scratch;
    }

    public Color[][] getScratch() {
        return scratch;
    }

    public Color[][] getScreen() {
        return screen;
    }

    public void updateScreenBuffer() {
        synchronized (lock) {
            Color[][] tmp = screen;
            screen = passive;
            passive = tmp;
        }
    }

    public void swapScratchBuffers() {
        synchronized (lock) {
            Color[][] tmp = scratch;
            scratch = passive;
            passive = tmp;
        }
    }
}
