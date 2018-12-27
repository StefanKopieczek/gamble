package com.kopieczek.gamble.hardware.memory;

import java.util.ArrayList;

public class VramModule extends RamModule implements Vram {
    private final ArrayList<SpriteChangeListener> spriteListeners = new ArrayList<>();

    VramModule() {
        super(Mmu.VRAM_SIZE);
    }

    @Override
    public void register(SpriteChangeListener listener) {
        spriteListeners.add(listener);
    }

    @Override
    public int[] getPatternBytes(int patternIndex) {
        if (patternIndex > 255) {
            throw new IllegalArgumentException("Pattern index " + patternIndex + " out of range (max 255)");
        }

        int[] pattern = new int[16];
        int startAddress = patternIndex * 16;
        for (int offset = 0; offset < 16; offset++) {
            pattern[offset] = readByte(startAddress + offset);
        }
        return pattern;
    }

    @Override
    public void setByte(int address, int value) {
        int prevValue = readByte(address);
        super.setByte(address, value);
        if (prevValue != value && address < 0x1000) {
            fireSpritePatternChanged(address / 16);
        }
    }

    private void fireSpritePatternChanged(int patternIndex) {
        spriteListeners.forEach(l -> l.onSpritePatternModified(patternIndex));
    }
}
