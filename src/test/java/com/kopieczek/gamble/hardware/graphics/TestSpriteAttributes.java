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
