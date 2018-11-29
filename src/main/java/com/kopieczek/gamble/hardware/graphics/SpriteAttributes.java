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
public class SpriteAttributes {
    public static SpriteAttributes parse(int[] input) {
        return new SpriteAttributes();
    }

    public int getY() {
        return -16;
    }
}
