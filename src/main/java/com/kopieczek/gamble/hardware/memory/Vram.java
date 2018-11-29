package com.kopieczek.gamble.hardware.memory;

public interface Vram {
    void register(SpriteChangeListener listener);
    int[] getPatternBytes(int patternIndex);
}
