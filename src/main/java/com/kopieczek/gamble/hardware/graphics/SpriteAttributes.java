package com.kopieczek.gamble.hardware.graphics;

/**
 * SpriteAttributes are made up of four contiguous bytes in OAM.
 * Byte 1: Sprite Y coordinate, minus 16
 * Byte 2: Sprite X coordinate, minus 8
 * Byte 3: Sprite pattern index, 0<=idx<256
 * Byte 4:
 *   - Bit 7:    Foreground/Background (Hide behind colors 1-3 if high)
 *   - Bit 6:    Y flip pattern if high
 *   - Bit 5:    X flip pattern if high
 *   - Bit 4:    Palette select
 *   - Bits 3-0: Unused on DMG
 */
class SpriteAttributes {
    private int x;
    private int y;

    private SpriteAttributes(int x, int y) {
        this.x = x;
        this.y = y;
    }

    static SpriteAttributes parse(int[] bytes) {
        return new SpriteAttributes(
                parseX(bytes),
                parseY(bytes)
        );
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    private static int parseX(int[] bytes) {
        return bytes[1] - 8;
    }

    private static int parseY(int[] bytes) {
        return bytes[0] - 16;
    }
}
