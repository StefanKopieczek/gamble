package com.kopieczek.gamble.hardware.graphics;

class SpritePattern {
    private final byte[][] pattern;

    private SpritePattern(byte[][] uncompressedPattern) {
        this.pattern = uncompressedPattern;
    }

    public byte[][] getUncompressedPattern() {
        return pattern;
    }

    static SpritePattern fromCompressed(int[] compressedBytes) {
        byte[][] uncompressedPattern = new byte[8][];

        for (int rowIdx = 0; rowIdx < 8; rowIdx++) {
            byte[] rowData = new byte[8];
            int mask = 0x80;
            int rowStart = rowIdx * 2;
            for (int idx = 0; idx < 8; idx++) {
                boolean lowBit = (compressedBytes[rowStart] & mask) > 0;
                boolean highBit = (compressedBytes[rowStart + 1] & mask) > 0;
                rowData[idx] = (byte) ((highBit ? 2 : 0) + (lowBit ? 1 : 0));
                mask >>= 1;
            }

            uncompressedPattern[rowIdx] = rowData;
        }

        return new SpritePattern(uncompressedPattern);
    }
}
