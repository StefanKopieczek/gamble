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
    Sprite(SpriteAttributes attributes, SpritePattern pattern) {
        this(attributes, getRows(pattern, attributes.getVerticalOrientation()));
    }

    /**
     * Constructor for tall sprites
     * @param attributes
     * @param pattern1 The topmost pattern of the sprite, when unflipped.
     * @param pattern2 The bottommost pattern of the sprite, when unflipped.
     */
    Sprite(SpriteAttributes attributes, SpritePattern pattern1, SpritePattern pattern2) {
        this(attributes, getRows(pattern1, pattern2, attributes.getVerticalOrientation()));
    }

    private Sprite(SpriteAttributes attributes, byte[][] rows) {
        this.attributes = attributes;
        pixels = new Color[rows.length][];
        int rowIdx = 0;
        for (byte[] row : rows) {
            int colIdx = 0;
            for (byte b : getCells(row, attributes.getHorizontalOrientation())) {
                pixels[rowIdx][colIdx] = getColor(b, attributes);
                colIdx++;
            }
            rowIdx++;
        }
    }

    public SpriteAttributes getAttributes() {
        return attributes;
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
                result[idx] =  original[8 - idx];
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
                result[idx] = row[8 - idx];
            }
        }

        return result;
    }

    private Color getColor(byte cell, SpriteAttributes attributes) {
        switch (cell) {
            case 3:
                return Color.BLACK;
            case 2:
                return new Color(192, 192, 192);
            case 1:
                return new Color(96, 96, 96);
            default:
                return Color.WHITE;
        }
    }
}
