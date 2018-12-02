package com.kopieczek.gamble.hardware.graphics;

import com.kopieczek.gamble.hardware.memory.Io;
import com.kopieczek.gamble.hardware.memory.Oam;
import com.kopieczek.gamble.hardware.memory.SpriteChangeListener;
import com.kopieczek.gamble.hardware.memory.Vram;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

class SpriteMap implements SpriteChangeListener {
    private final Io io;
    private final Oam oam;
    private final Vram vram;
    private final List<SpriteAttributes> allAttributes = new ArrayList<>();
    private final List<SpritePattern> patterns = new ArrayList<>();
    private final List<Sprite> sprites = new ArrayList<>();

    private boolean useTallSprites = false;

    SpriteMap(Io io, Oam oam, Vram vram) {
        this.io = io;
        this.oam = oam;
        this.vram = vram;
        io.register(this);
        oam.register(this);
        vram.register(this);
    }

    public void init() {
        reloadAll();
    }

    private void reloadAll() {
        reloadAllAttributes();
        reloadPatterns();
        loadSpriteHeight();
        rebuildSprites();
    }

    private void reloadAllAttributes() {
        allAttributes.clear();
        IntStream.range(0, Oam.TOTAL_ATTRIBUTES).forEach(idx -> {
            int[] data = oam.getAttributeBytes(idx);
            allAttributes.add(SpriteAttributes.parse(data));
        });
    }

    private void reloadPatterns() {
        patterns.clear();
        IntStream.range(0, Vram.TOTAL_SPRITE_PATTERNS).forEach(idx -> {
            int[] compressedPattern = vram.getPatternBytes(idx);
            patterns.add(SpritePattern.fromCompressed(compressedPattern));
        });
    }

    private void loadSpriteHeight() {
        useTallSprites = (io.getSpriteHeight() == 16);
    }

    private void rebuildSprites() {
        sprites.clear();
        allAttributes.forEach(attributes -> {
            if (useTallSprites) {
                sprites.add(buildTallSprite(attributes));
            } else {
                sprites.add(buildShortSprite(attributes));
            }
        });
    }

    private Sprite buildShortSprite(SpriteAttributes attributes) {
        SpritePattern pattern = patterns.get(attributes.getPatternIndex());
        return new Sprite(attributes, pattern);
    }

    private Sprite buildTallSprite(SpriteAttributes attributes) {
        int pattern1Index = (attributes.getPatternIndex()) & 0xfe;
        int pattern2Index = (attributes.getPatternIndex()) | 0x01;
        SpritePattern pattern1 = patterns.get(pattern1Index);
        SpritePattern pattern2 = patterns.get(pattern2Index);
        return new Sprite(attributes, pattern1, pattern2);
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
