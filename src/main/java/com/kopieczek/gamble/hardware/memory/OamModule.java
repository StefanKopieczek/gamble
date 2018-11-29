package com.kopieczek.gamble.hardware.memory;

import java.util.ArrayList;

class OamModule extends RamModule implements Oam {
    private final ArrayList<SpriteChangeListener> spriteListeners = new ArrayList<>();

    OamModule() {
        super(Mmu.OAM_SIZE);
    }

    @Override
    public void register(SpriteChangeListener listener) {
        spriteListeners.add(listener);
    }

    @Override
    public int[] getAttributeBytes(int spriteIndex) {
        int[] attributes = new int[4];
        int startAddress = spriteIndex * 4;
        for (int offset = 0; offset < 4; offset++) {
            attributes[offset] = readByte(startAddress + offset);
        }
        return attributes;
    }

    @Override
    public void setByte(int address, int value) {
        int prevValue = readByte(address);
        super.setByte(address, value);
        if (prevValue != value) {
            fireSpriteAttributesChanged(address / 4);
        }
    }

    private void fireSpriteAttributesChanged(int spriteIndex) {
        spriteListeners.forEach(l -> l.onSpriteAttributesModified(spriteIndex));
    }
}
