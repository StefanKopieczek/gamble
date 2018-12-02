package com.kopieczek.gamble.hardware.memory;

public interface Vram {
    int TOTAL_SPRITE_PATTERNS = 256;

    void register(SpriteChangeListener listener);
    int[] getPatternBytes(int patternIndex);
}
