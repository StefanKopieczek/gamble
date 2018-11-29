package com.kopieczek.gamble.hardware.memory;

public interface Oam {
    void register(SpriteChangeListener listener);
    int[] getAttributeBytes(int spriteIndex);
}
