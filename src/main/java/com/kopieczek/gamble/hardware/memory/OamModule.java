package com.kopieczek.gamble.hardware.memory;

import java.util.ArrayList;

class OamModule extends RamModule {
    private final ArrayList<SpriteChangeListener> spriteListeners = new ArrayList<>();

    OamModule() {
        super(Mmu.OAM_SIZE);
    }

    public void register(SpriteChangeListener listener) {
        spriteListeners.add(listener);
    }

    @Override
    public void setByte(int address, int value) {
        int prevValue = readByte(address);
        super.setByte(address, value);
        if (prevValue != value) {
            fireSpritePatternModified(address / 4);
        }
    }

    private void fireSpritePatternModified(int spriteIndex) {
        spriteListeners.forEach(l -> l.onSpritePatternModified(spriteIndex));
    }
}
