package com.kopieczek.gamble.graphics;

import com.kopieczek.gamble.memory.MemoryManagementUnit;

import java.awt.*;
import java.util.Random;

public class Gpu {
    public static final int DISPLAY_WIDTH = 160;
    public static final int DISPLAY_HEIGHT= 144;
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
        }
    }

    private void renderLine(int currentLine) {
        Random r = new Random();
        synchronized (screenBuffer) {
            for (int x = 0; x < DISPLAY_WIDTH; x++) {
                screenBuffer[currentLine][x] = new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256));
            }
        }
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
}
