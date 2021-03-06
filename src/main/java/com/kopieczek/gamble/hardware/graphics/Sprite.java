package com.kopieczek.gamble.hardware.graphics;

import java.awt.*;
import java.util.Arrays;

class Sprite {
    private final Color[][] pixels;
    private final SpriteAttributes attributes;

    /**
     * Constructor for short sprites
     * @param attributes
     * @param pattern
     */
    Sprite(SpriteAttributes attributes, SpritePattern pattern, Color[] palette0, Color[] palette1) {
        this(attributes, getRows(pattern, attributes.getVerticalOrientation()), palette0, palette1);
    }

    /**
     * Constructor for tall sprites
     * @param attributes
     * @param pattern1 The topmost pattern of the sprite, when unflipped.
     * @param pattern2 The bottommost pattern of the sprite, when unflipped.
     */
    Sprite(SpriteAttributes attributes, SpritePattern pattern1, SpritePattern pattern2, Color[] palette0, Color[] palette1) {
        this(attributes, getRows(pattern1, pattern2, attributes.getVerticalOrientation()), palette0, palette1);
    }

    private Sprite(SpriteAttributes attributes, byte[][] rows, Color[] palette0, Color[] palette1) {
        this.attributes = attributes;
        pixels = new Color[rows.length][];
        int rowIdx = 0;
        for (byte[] row : rows) {
            pixels[rowIdx] = new Color[8];
            int colIdx = 0;
            for (byte b : getCells(row, attributes.getHorizontalOrientation())) {
                pixels[rowIdx][colIdx] = getColor(b, attributes, palette0, palette1);
                colIdx++;
            }
            rowIdx++;
        }
    }

    SpriteAttributes getAttributes() {
        return attributes;
    }

    Color[][] getPixels() {
        return pixels;
    }

    private static byte[][] getRows(SpritePattern pattern1,
                                    SpritePattern pattern2,
                                    SpriteAttributes.Orientation verticalOrientation) {
        // Strategy: the array of rows for a tall sprite can be built from the rows of its top and bottom subsprites.
        // If the sprite is unflipped, we just return the first subsprite's rows, followed by those of the second.
        // If the entire sprite is flipped, we return the reversed rows of the second subsprite, followed by those of the first.
        byte[][] pattern1Rows = getRows(pattern1, verticalOrientation);
        byte[][] pattern2Rows = getRows(pattern2, verticalOrientation);

        byte[][] top;
        byte[][] bottom;
        if (verticalOrientation == SpriteAttributes.Orientation.UNCHANGED) {
            top = pattern1Rows;
            bottom = pattern2Rows;
        } else {
            top = pattern2Rows;
            bottom = pattern1Rows;
        }

        byte[][] result = Arrays.copyOf(top, top.length + bottom.length);
        System.arraycopy(bottom, 0, result, top.length, bottom.length);
        return result;
    }

    private static byte[][] getRows(SpritePattern pattern, SpriteAttributes.Orientation verticalOrientation) {
        byte[][] result;
        if (verticalOrientation == SpriteAttributes.Orientation.UNCHANGED) {
            result = pattern.getUncompressedPattern();
        } else {
            byte[][] original = pattern.getUncompressedPattern();
            result = new byte[8][];
            for (int idx = 0; idx < 8; idx++) {
                result[idx] =  original[7 - idx];
            }
        }

        return result;
    }

    private static byte[] getCells(byte[] row, SpriteAttributes.Orientation horizontalOrientation) {
        byte[] result;
        if (horizontalOrientation == SpriteAttributes.Orientation.UNCHANGED) {
            result = row;
        } else {
            result = new byte[8];
            for (int idx = 0; idx < 8; idx++) {
                result[idx] = row[7 - idx];
            }
        }

        return result;
    }

    private Color getColor(byte cell, SpriteAttributes attributes, Color[] palette0, Color[] palette1) {
        if (attributes.getPalette() == 0) {
            return palette0[cell];
        } else {
            return palette1[cell];
        }
    }

    public int getAttributeIndex() {
        return attributes.getIndex();
    }

    public boolean isTall() {
        return pixels.length > 8;
    }
}
