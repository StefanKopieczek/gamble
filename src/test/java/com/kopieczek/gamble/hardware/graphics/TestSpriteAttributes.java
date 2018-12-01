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
            SpriteAttributes attributes = SpriteAttributes.parse(input);
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
