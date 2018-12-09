package com.kopieczek.gamble.hardware.graphics;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.kopieczek.gamble.hardware.memory.Io;
import com.kopieczek.gamble.hardware.memory.Oam;
import com.kopieczek.gamble.hardware.memory.SpriteChangeListener;
import com.kopieczek.gamble.hardware.memory.Vram;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class SpriteMap implements SpriteChangeListener {
    private static final Logger log = LogManager.getLogger(SpriteMap.class);
    private final Io io;
    private final Oam oam;
    private final Vram vram;
    private final List<SpriteAttributes> allAttributes = new ArrayList<>();
    private final List<SpritePattern> patterns = new ArrayList<>();
    private final List<Sprite> sprites = new ArrayList<>();
    private final Set<Integer> dirtyAttributes = new HashSet<>();
    private final Set<Integer> dirtyPatterns = new HashSet<>();
    private final Set<Integer> dirtySprites = new HashSet<>();

    // NB: The following fields are only guaranteed to be up to date when dirtyAttributes is empty.
    private final Multimap<Integer, Integer> patternToSpriteMap = ArrayListMultimap.create();
    private final Multimap<Integer, Integer> rowToSpriteMap = ArrayListMultimap.create();

    private boolean useTallSprites = false;
    private Color[] palette0;
    private Color[] palette1;

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
        reloadAllPatterns();
        loadSpriteHeight();
        loadPalettes();
        rebuildAllSprites();
    }

    private void loadPalettes() {
        palette0 = io.loadPalette0();
        palette1 = io.loadPalette1();
    }

    private void reloadAllAttributes() {
        allAttributes.clear();
        dirtyAttributes.clear();
        patternToSpriteMap.clear();
        IntStream.range(0, Oam.TOTAL_ATTRIBUTES).forEach(idx -> {
            int[] data = oam.getAttributeBytes(idx);
            SpriteAttributes attrs = SpriteAttributes.parse(idx, data);
            allAttributes.add(attrs);
            getPatternsUsed(attrs).forEach(pattern -> patternToSpriteMap.put(pattern, idx));
            getRowsUsed(attrs).forEach(row -> rowToSpriteMap.put(row, idx));
        });
    }

    private void reloadDirtyAttributes() {
        reloadAttributes(dirtyAttributes);
    }

    private void reloadAttributes(Collection<Integer> attrsToReload) {
        attrsToReload.forEach(spriteIndex -> {
            // Remove existing attribute based-mappings for this sprite.
            Sprite oldSprite = sprites.get(spriteIndex);
            getPatternsUsed(oldSprite).forEach(pattern -> patternToSpriteMap.remove(pattern, spriteIndex));
            getRowsUsed(oldSprite).forEach(row -> rowToSpriteMap.remove(row, spriteIndex));

            // Reload attributes
            int[] data = oam.getAttributeBytes(spriteIndex);
            SpriteAttributes newAttrs = SpriteAttributes.parse(spriteIndex, data);
            allAttributes.set(spriteIndex, newAttrs);

            // Add new attribute-based mappings for the reloaded value.
            getPatternsUsed(newAttrs).forEach(pattern -> patternToSpriteMap.put(pattern, spriteIndex));
            getRowsUsed(newAttrs).forEach(row -> rowToSpriteMap.put(row, spriteIndex));
        });
        dirtyAttributes.removeAll(attrsToReload);
    }

    private void reloadAllPatterns() {
        assert(dirtyAttributes.size() == 0);
        patterns.clear();
        dirtyPatterns.clear();
        IntStream.range(0, Vram.TOTAL_SPRITE_PATTERNS).forEach(patternIdx -> {
            int[] compressedPattern = vram.getPatternBytes(patternIdx);
            patterns.add(SpritePattern.fromCompressed(compressedPattern));
            dirtySprites.addAll(patternToSpriteMap.get(patternIdx));
        });
    }

    private void reloadPatterns(Collection<Integer> patternsToReload) {
        assert(dirtyAttributes.size() == 0);
        patternsToReload.forEach(patternIdx -> {
            int[] compressedPattern = vram.getPatternBytes(patternIdx);
            patterns.set(patternIdx, SpritePattern.fromCompressed(compressedPattern));
            dirtySprites.addAll(patternToSpriteMap.get(patternIdx));
        });
    }

    private void ensureSpritesAreClean(Collection<Integer> spriteIndexes) {
        reloadDirtyAttributes();

        Set<Integer> patternsToReload = spriteIndexes.stream()
                .map(allAttributes::get)
                .flatMap(sprite -> getPatternsUsed(sprite).stream())
                .filter(dirtyPatterns::contains)
                .collect(Collectors.toSet());
        reloadPatterns(patternsToReload);

        List<Integer> spritesToRebuild = spriteIndexes.stream().filter(dirtySprites::contains).collect(Collectors.toList());
        rebuildSprites(spritesToRebuild);
    }

    private void ensureRowIsClean(int rowIdx) {
        // We have to validate attributes first, or we can't reliably tell which sprites are on the row.
        reloadDirtyAttributes();

        Collection<Integer> spritesOnRow = rowToSpriteMap.get(rowIdx);
        ensureSpritesAreClean(spritesOnRow);
    }

    /**
     * Return all sprites that overlap the given row, in descending priority order.
     * Priority is defined by the following rules:
     *  1) Sprites with a lower x coordinate have a higher priority
     *  2) If sprites are equal under (1), the sprite with the lower attribute index has the higher priority
     * @param rowIdx The row in question
     * @return Sprites overlapping the given row, in descending priority order.
     */
    List<Sprite> getSpritesForRow(int rowIdx) {
        ensureRowIsClean(rowIdx);
        return rowToSpriteMap.get(rowIdx).stream()
                .map(sprites::get)
                .sorted(Comparator.comparingInt(Sprite::getAttributeIndex))
                .sorted(Comparator.comparingInt(sprite -> sprite.getAttributes().getX()))
                .collect(Collectors.toList());
    }

    private void loadSpriteHeight() {
        useTallSprites = (io.getSpriteHeight() == 16);
    }

    private void rebuildAllSprites() {
        sprites.clear();
        dirtySprites.clear();
        allAttributes.forEach(attributes -> {
            if (useTallSprites) {
                sprites.add(buildTallSprite(attributes));
            } else {
                sprites.add(buildShortSprite(attributes));
            }
        });
    }

    private void rebuildSprites(Collection<Integer> spritesToRebuild) {
        spritesToRebuild.forEach(spriteIdx -> {
            SpriteAttributes attributes = allAttributes.get(spriteIdx);
            Sprite sprite;
            if (useTallSprites) {
                sprite = buildTallSprite(attributes);
            } else {
                sprite = buildShortSprite(attributes);
            }
            sprites.set(spriteIdx, sprite);
        });
        dirtySprites.removeAll(spritesToRebuild);
    }

    private Sprite buildShortSprite(SpriteAttributes attributes) {
        SpritePattern pattern = patterns.get(attributes.getPatternIndex());
        return new Sprite(attributes, pattern, palette0, palette1);
    }

    private Sprite buildTallSprite(SpriteAttributes attributes) {
        int pattern1Index = (attributes.getPatternIndex()) & 0xfe;
        int pattern2Index = (attributes.getPatternIndex()) | 0x01;
        SpritePattern pattern1 = patterns.get(pattern1Index);
        SpritePattern pattern2 = patterns.get(pattern2Index);
        return new Sprite(attributes, pattern1, pattern2, palette0, palette1);
    }

    private Set<Integer> getPatternsUsed(SpriteAttributes attrs) {
        int mainPattern = attrs.getPatternIndex();
        if (useTallSprites) {
            int twinnedPattern = mainPattern ^ 0xfe;  // Flip final bit
            return ImmutableSet.of(mainPattern, twinnedPattern);
        } else {
            return ImmutableSet.of(mainPattern);
        }
    }

    private Set<Integer> getPatternsUsed(Sprite sprite) {
        int mainPattern = sprite.getAttributes().getPatternIndex();
        if (sprite.isTall()) {
            int twinnedPattern = mainPattern ^ 0xfe;
            return ImmutableSet.of(mainPattern, twinnedPattern);
        } else {
            return ImmutableSet.of(mainPattern);
        }
    }

    private Set<Integer> getRowsUsed(SpriteAttributes attrs) {
        int numRows = useTallSprites ? 16 : 8;
        int minRow = attrs.getY();
        int maxRow = minRow + numRows;
        return IntStream.range(minRow, maxRow).boxed().collect(Collectors.toSet());
    }

    private Set<Integer> getRowsUsed(Sprite sprite) {
        int numRows = sprite.isTall() ? 16 : 8;
        int minRow = sprite.getAttributes().getY();
        int maxRow = minRow + numRows;
        return IntStream.range(minRow, maxRow).boxed().collect(Collectors.toSet());
    }

    @Override
    public void onSpriteAttributesModified(int spriteIndex) {
        dirtyAttributes.add(spriteIndex);
        dirtySprites.add(spriteIndex);
    }

    @Override
    public void onSpritePatternModified(int patternIndex) {
        dirtyPatterns.add(patternIndex);
        // We can't reliably mark the associated sprites dirty here, since there may be outstanding dirty attributes,
        // in which case it's not possible to determine which sprites are using which patterns.
        // Instead we enforce the following:
        //   1. When ensuring sprites are clean, they must explicitly check to see if their pattern(s) are dirty.
        //   2. Reloading patterns may only occur when all attributes are clean.
        //   3. When a pattern is reloaded, we must immediately mark all sprites using the pattern as dirty.
        //      (We can determine the set of affected sprites reliably since by 2. the attributes are all clean)
    }

    @Override
    public void onSpriteHeightChanged(boolean areTallSpritesEnabled) {
        log.debug("Tall sprites status changed. Enabled=" + areTallSpritesEnabled);
        useTallSprites = areTallSpritesEnabled;
        IntStream.range(0, Oam.TOTAL_ATTRIBUTES).forEach(dirtyAttributes::add);
    }

    @Override
    public void onSpritePaletteChanged() {
        palette0 = io.loadPalette0();
        palette1 = io.loadPalette1();
        IntStream.range(0, Vram.TOTAL_SPRITE_PATTERNS).forEach(dirtyPatterns::add);
    }
}
