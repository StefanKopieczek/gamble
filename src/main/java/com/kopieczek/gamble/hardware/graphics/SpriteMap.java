package com.kopieczek.gamble.hardware.graphics;

import com.kopieczek.gamble.hardware.memory.Io;
import com.kopieczek.gamble.hardware.memory.Oam;
import com.kopieczek.gamble.hardware.memory.SpriteChangeListener;
import com.kopieczek.gamble.hardware.memory.Vram;

class SpriteMap implements SpriteChangeListener {
    private final Oam oam;
    private final Vram vram;

    SpriteMap(Io io, Oam oam, Vram vram) {
        this.oam = oam;
        this.vram = vram;
        io.register(this);
        oam.register(this);
        vram.register(this);
    }

    @Override
    public void onSpriteAttributesModified(int spriteIndex) {

    }

    @Override
    public void onSpritePatternModified(int patternIndex) {

    }

    @Override
    public void onSpriteHeightChanged(boolean areTallSpritesEnabled) {

    }
}
