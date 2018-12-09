package com.kopieczek.gamble.hardware.memory;

public interface SpriteChangeListener {
    void onSpriteAttributesModified(int spriteIndex);
    void onSpritePatternModified(int patternIndex);
    void onSpriteHeightChanged(boolean areTallSpritesEnabled);
    void onSpritePaletteChanged();
}
