package com.kopieczek.gamble.hardware.graphics;

/**
 * SpriteAttributes are made up of four contiguous bytes in OAM.
 * Byte 1: Sprite Y coordinate, minus 16
 * Byte 2: Sprite X coordinate, minus 8
 * Byte 3: Sprite pattern index, 0<=idx<256
 * Byte 4:
 *   - Bit 7:    Foreground/Background (Hide behind colors 1-3 if high)
 *   - Bit 6:    Vertically flip pattern if high
 *   - Bit 5:    Horizontally flip pattern if high
 *   - Bit 4:    Palette select
 *   - Bits 3-0: Unused on DMG
 */
class SpriteAttributes {
    private final int index;
    private final int x;
    private final int y;
    private final int patternIndex;
    private final ZPosition zPosition;
    private final Orientation verticalOrientation;
    private final Orientation horizontalOrientation;
    private final int palette;

    private SpriteAttributes(int index, int x, int y, int patternIndex, ZPosition zPosition,
                             Orientation horizontalOrientation, Orientation verticalOrientation,
                             int paletteIndex) {
        this.index = index;
        this.x = x;
        this.y = y;
        this.patternIndex = patternIndex;
        this.zPosition = zPosition;
        this.horizontalOrientation = horizontalOrientation;
        this.verticalOrientation = verticalOrientation;
        this.palette = paletteIndex;
    }

    static SpriteAttributes parse(int index, int[] bytes) {
        return new SpriteAttributes(
                index,
                parseX(bytes),
                parseY(bytes),
                parsePatternIndex(bytes),
                parseZPosition(bytes),
                parseHorizontalOrientation(bytes),
                parseVerticalOrientation(bytes),
                parsePalette(bytes)
        );
    }

    int getIndex() {
        return index;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getPatternIndex() {
        return patternIndex;
    }

    ZPosition getZPosition() {
        return zPosition;
    }

    Orientation getHorizontalOrientation() {
        return horizontalOrientation;
    }

    Orientation getVerticalOrientation() {
        return verticalOrientation;
    }

    int getPalette() {
        return palette;
    }

    private static int parseX(int[] bytes) {
        return bytes[1] - 8;
    }

    private static int parseY(int[] bytes) {
        return bytes[0] - 16;
    }

    private static int parsePatternIndex(int[] bytes) {
        return bytes[2];
    }

    private static ZPosition parseZPosition(int[] bytes) {
        return (bytes[3] & 0x80) > 0 ? ZPosition.BACKGROUND : ZPosition.FOREGROUND;
    }

    private static Orientation parseHorizontalOrientation(int[] bytes) {
        return (bytes[3] & 0x20) > 0 ? Orientation.FLIPPED : Orientation.UNCHANGED;
    }

    private static Orientation parseVerticalOrientation(int[] bytes) {
        return (bytes[3] & 0x40) > 0 ? Orientation.FLIPPED : Orientation.UNCHANGED;
    }

    private static int parsePalette(int[] bytes) {
        return (bytes[3] & 0x10) > 0 ? 1 : 0;
    }

    enum ZPosition {
        FOREGROUND,
        BACKGROUND
    }

    enum Orientation {
        UNCHANGED,
        FLIPPED
    }
}
