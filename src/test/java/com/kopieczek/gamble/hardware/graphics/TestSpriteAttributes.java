package com.kopieczek.gamble.hardware.graphics;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class TestSpriteAttributes {
    @Test
    public void test_y_coord() {
        TableTest.of(SpriteAttributes::getY)
            .testCase(-16, 0x00, 0x00, 0x00, 0x00)
            .testCase(-15, 0x01, 0x00, 0x00, 0x00)
            .testCase(-14, 0x02, 0x00, 0x00, 0x00)
            .testCase( -1, 0x0f, 0x00, 0x00, 0x00)
            .testCase(  0, 0x10, 0x00, 0x00, 0x00)
            .testCase(  1, 0x11, 0x00, 0x00, 0x00)
            .testCase( 10, 0x1a, 0x00, 0x00, 0x00)
            .testCase( 30, 0x2e, 0x00, 0x00, 0x00)
            .testCase(239, 0xff, 0x00, 0x00, 0x00)
            .testCase(-16, 0x00, 0xff, 0xff, 0xff)
            .testCase(  0, 0x10, 0xff, 0xff, 0xff)
            .testCase(200, 0xd8, 0xff, 0xff, 0xff)
            .testCase(-16, 0x00, 0x12, 0x34, 0x56)
            .testCase(  0, 0x10, 0x12, 0x34, 0x56)
            .testCase(200, 0xd8, 0x12, 0x34, 0x56)
            .run();
    }

    @Test
    public void test_x_coord() {
        TableTest.of(SpriteAttributes::getX)
            .testCase( -8, 0x00, 0x00, 0x00, 0x00)
            .testCase( -7, 0x00, 0x01, 0x00, 0x00)
            .testCase( -6, 0x00, 0x02, 0x00, 0x00)
            .testCase(  7, 0x00, 0x0f, 0x00, 0x00)
            .testCase(  8, 0x00, 0x10, 0x00, 0x00)
            .testCase(  9, 0x00, 0x11, 0x00, 0x00)
            .testCase( 18, 0x00, 0x1a, 0x00, 0x00)
            .testCase( 38, 0x00, 0x2e, 0x00, 0x00)
            .testCase(247, 0x00, 0xff, 0x00, 0x00)
            .testCase( -8, 0xff, 0x00, 0xff, 0xff)
            .testCase(  8, 0xff, 0x10, 0xff, 0xff)
            .testCase(208, 0xff, 0xd8, 0xff, 0xff)
            .testCase( -8, 0x12, 0x00, 0x34, 0x56)
            .testCase(  8, 0x12, 0x10, 0x34, 0x56)
            .testCase(208, 0x12, 0xd8, 0x34, 0x56)
            .run();
    }

    @Test
    public void test_sprite_pattern_index() {
        TableTest.of(SpriteAttributes::getPatternIndex)
            .testCase(0x00, 0x00, 0x00, 0x00, 0x00)
            .testCase(0x01, 0x00, 0x00, 0x01, 0x00)
            .testCase(0x02, 0x00, 0x00, 0x02, 0x00)
            .testCase(0x03, 0x00, 0x00, 0x03, 0x00)
            .testCase(0x07, 0x00, 0x00, 0x07, 0x00)
            .testCase(0x0a, 0x00, 0x00, 0x0a, 0x00)
            .testCase(0x34, 0x00, 0x00, 0x34, 0x00)
            .testCase(0x9d, 0x00, 0x00, 0x9d, 0x00)
            .testCase(0xc0, 0x00, 0x00, 0xc0, 0x00)
            .testCase(0xff, 0x00, 0x00, 0xff, 0x00)
            .testCase(0x00, 0xff, 0xff, 0x00, 0xff)
            .testCase(0x1e, 0xff, 0xff, 0x1e, 0xff)
            .testCase(0x65, 0xab, 0xcd, 0x65, 0xef)
            .run();
    }

    @Test
    public void test_z_position() {
        TableTest.of(SpriteAttributes::getZPosition)
            .testCase(SpriteAttributes.ZPosition.FOREGROUND, 0x00, 0x00, 0x00, 0b00000000)
            .testCase(SpriteAttributes.ZPosition.BACKGROUND, 0x00, 0x00, 0x00, 0b10000000)
            .testCase(SpriteAttributes.ZPosition.FOREGROUND, 0xff, 0xff, 0xff, 0b00000000)
            .testCase(SpriteAttributes.ZPosition.BACKGROUND, 0xff, 0xff, 0xff, 0b10000000)
            .testCase(SpriteAttributes.ZPosition.FOREGROUND, 0x12, 0x34, 0x56, 0b00000000)
            .testCase(SpriteAttributes.ZPosition.BACKGROUND, 0x12, 0x34, 0x56, 0b10000000)
            .testCase(SpriteAttributes.ZPosition.FOREGROUND, 0x00, 0x00, 0x00, 0b01111111)
            .testCase(SpriteAttributes.ZPosition.BACKGROUND, 0x00, 0x00, 0x00, 0b11111111)
            .testCase(SpriteAttributes.ZPosition.FOREGROUND, 0x00, 0x00, 0x00, 0b00010101)
            .testCase(SpriteAttributes.ZPosition.BACKGROUND, 0x00, 0x00, 0x00, 0b10010101)
            .testCase(SpriteAttributes.ZPosition.FOREGROUND, 0x00, 0x00, 0x00, 0b01101010)
            .testCase(SpriteAttributes.ZPosition.BACKGROUND, 0x00, 0x00, 0x00, 0b11101010)
            .run();
    }

    @Test
    public void test_vertical_orientation() {
        TableTest.of(SpriteAttributes::getVerticalOrientation)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x00, 0x00, 0x00, 0b00000000)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x00, 0x00, 0x00, 0b01000000)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0xff, 0xff, 0xff, 0b00000000)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0xff, 0xff, 0xff, 0b01000000)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x12, 0x34, 0x56, 0b00000000)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x12, 0x34, 0x56, 0b01000000)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x00, 0x00, 0x00, 0b10111111)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x00, 0x00, 0x00, 0b11111111)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x00, 0x00, 0x00, 0b00010101)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x00, 0x00, 0x00, 0b01010101)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x00, 0x00, 0x00, 0b10101010)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x00, 0x00, 0x00, 0b11101010)
            .run();
    }


    @Test
    public void test_horizontal_orientation() {
        TableTest.of(SpriteAttributes::getHorizontalOrientation)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x00, 0x00, 0x00, 0b00000000)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x00, 0x00, 0x00, 0b00100000)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0xff, 0xff, 0xff, 0b00000000)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0xff, 0xff, 0xff, 0b00100000)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x12, 0x34, 0x56, 0b00000000)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x12, 0x34, 0x56, 0b00100000)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x00, 0x00, 0x00, 0b11011111)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x00, 0x00, 0x00, 0b11111111)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x00, 0x00, 0x00, 0b00010101)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x00, 0x00, 0x00, 0b00110101)
            .testCase(SpriteAttributes.Orientation.UNCHANGED, 0x00, 0x00, 0x00, 0b11001010)
            .testCase(SpriteAttributes.Orientation.FLIPPED,   0x00, 0x00, 0x00, 0b11101010)
            .run();
    }

    private static class TableTest<T> {
        private final Function<SpriteAttributes, T> methodToTest;
        private List<TestRow<T>> table = new ArrayList<>();

        private TableTest(Function<SpriteAttributes, T> methodToTest) {
            this.methodToTest = methodToTest;
        }

        void run() {
            table.forEach(row -> {
                row.doTest(methodToTest);
            });
        }

        static <T> TableTest<T> of(Function<SpriteAttributes, T> method) {
            return new TableTest<>(method);
        }

        TableTest<T> testCase(T expected, int... input) {
            table.add(new TestRow<>(expected, input));
            return this;
        }
    }

    private static class TestRow<T> {
        public final T expected;
        public final int[] input;

        public TestRow(T expected, int... input) {
            this.expected = expected;
            this.input = input;
        }

        public void doTest(Function<SpriteAttributes, T> methodToTest) {
            SpriteAttributes attributes = SpriteAttributes.parse(0, input);
            assertEquals("Failure on " + intsToHex(input), expected, methodToTest.apply(attributes));
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String intsToHex(int[] ints) {
        char[] hexChars = new char[ints.length * 2];
        for ( int j = 0; j < ints.length; j++ ) {
            int v = ints[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
