package com.kopieczek.gamble.hardware.memory;

import java.util.ArrayList;

public class VramModule extends RamModule {
    private final ArrayList<SpriteChangeListener> spriteListeners = new ArrayList<>();

    VramModule() {
        super(Mmu.VRAM_SIZE);
    }

    public void register(SpriteChangeListener listener) {
        spriteListeners.add(listener);
    }

    @Override
    public void setByte(int address, int value) {
        int prevValue = readByte(address);
        super.setByte(address, value);
        if (prevValue != value) {
            fireSpritePatternChanged(address / 16);
        }
    }

    private void fireSpritePatternChanged(int patternIndex) {
        spriteListeners.forEach(l -> l.onSpritePatternModified(patternIndex));
    }
}
