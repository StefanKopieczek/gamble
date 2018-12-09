package com.kopieczek.gamble.hardware.memory;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class TestOamModule {
    private final int[] TEST_ATTRS_1 = new int[] {0x12, 0x34, 0x56, 0x78};
    private final int[] TEST_ATTRS_2 = new int[] {0xf4, 0x32, 0xe3, 0x21};
    private final int[] TEST_ATTRS_3 = new int[] {0xcb, 0x99, 0x6b, 0x79};
    private final List<int[]> ALL_TEST_ATTRS = ImmutableList.of(TEST_ATTRS_1, TEST_ATTRS_2, TEST_ATTRS_3);

    @Test
    public void test_writes_fire_attributes_modified_events_for_correct_sprites() {
        final AtomicInteger lastModified = new AtomicInteger(-1);
        doSpriteAttributesChangeTest(
            oam -> {
                for (int address = 0x00; address < 0xa0; address++) {
                    oam.setByte(address, 0xff);
                    int expectedPattern = address / 4;
                    assertEquals(String.format("Expected write to 0x%02x to fire change on pattern %d",
                                               address, expectedPattern),
                            expectedPattern, lastModified.get());
                }
            },
            lastModified::set
        );
    }

    @Test
    public void test_writes_do_not_fire_pattern_modified_events() {
        SpriteChangeListener listener = new SpriteChangeAdapter() {
            @Override
            public void onSpritePatternModified(int patternIndex) {
                fail();
            }
        };

        OamModule oam = new OamModule();
        oam.register(listener);
        IntStream.range(0x00, 0xa0).forEach(addr -> oam.setByte(addr, 0xff));
    }

    @Test
    public void test_all_writes_fire_attributes_modified_events() {
        AtomicInteger numEvents = new AtomicInteger(0);
        doSpriteAttributesChangeTest(
            oam -> {
                for (int val = 0xff; val >= 0; val--) {
                    oam.setByte(0x00, val);
                }
            },
            changedSprite -> numEvents.incrementAndGet()
        );
        assertEquals(256, numEvents.get());
    }

    @Test
    public void test_initial_zero_write_does_not_fire_attributes_modified() {
        doSpriteAttributesChangeTest(
            oam -> oam.setByte(0x00, 0x00),
            changedSprite -> fail()
        );
    }

    @Test
    public void test_repeated_writes_only_fire_attributes_modified_once() {
        AtomicBoolean alreadyFired = new AtomicBoolean(false);
        doSpriteAttributesChangeTest(
            oam -> IntStream.range(0, 1000).forEach(x -> oam.setByte(0x00, 0xff)),
            changedSprite ->  {
                if (alreadyFired.getAndSet(true)) {
                    fail();
                }
            }
        );
    }

    @Test
    public void test_get_attribute_bytes() {
        assertAttributeReadHitsAddress(0, 0x00);
        assertAttributeReadHitsAddress(1, 0x04);
        assertAttributeReadHitsAddress(2, 0x08);
        assertAttributeReadHitsAddress(5, 0x14);
        assertAttributeReadHitsAddress(38, 0x98);
        assertAttributeReadHitsAddress(39, 0x9c);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_attribute_bytes_fails_for_index_negative_one() {
        new OamModule().getAttributeBytes(-1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_attribute_bytes_fails_for_index_negative_100() {
        new OamModule().getAttributeBytes(-100);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_attribute_bytes_fails_for_index_40() {
        new OamModule().getAttributeBytes(40);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_attribute_bytes_fails_for_index_45() {
        new OamModule().getAttributeBytes(45);
    }

    private void doSpriteAttributesChangeTest(Consumer<OamModule> test, Consumer<Integer> onAttributesChanged) {
        SpriteChangeListener listener = new SpriteChangeAdapter() {
            @Override
            public void onSpriteAttributesModified(int spriteIndex) {
                onAttributesChanged.accept(spriteIndex);
            }
        };

        OamModule oam = new OamModule();
        oam.register(listener);
        test.accept(oam);
    }

    private void assertAttributeReadHitsAddress(int spriteIndex, int address) {
        OamModule oam = new OamModule();
        ALL_TEST_ATTRS.forEach(attributes -> {
            for (int idx = 0; idx < 4; idx++) {
                oam.setByte(address + idx, attributes[idx]);
            }
            assertArrayEquals(attributes, oam.getAttributeBytes(spriteIndex));
        });
    }
}
