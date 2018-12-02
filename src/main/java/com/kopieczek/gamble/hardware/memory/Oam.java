package com.kopieczek.gamble.hardware.memory;

public interface Oam {
    final int TOTAL_ATTRIBUTES = 40;

    void register(SpriteChangeListener listener);
    int[] getAttributeBytes(int spriteIndex);
}
