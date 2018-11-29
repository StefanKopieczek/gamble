package com.kopieczek.gamble.hardware.memory;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class TestVramModule {
    private static final int[] SPRITE_1 = new int[] {
            0x12, 0x34, 0x56, 0x78, 0x9a, 0xbc, 0xde, 0xf0,
            0x10, 0x2f, 0x3e, 0x4d, 0x5c, 0x6b, 0x7a, 0x89
    };

    private static final int[] SPRITE_2 = new int[] {
            0x01, 0xf2, 0xe3, 0xd4, 0xc5, 0xb6, 0xa7, 0x98,
            0x20, 0x43, 0x65, 0x87, 0xa9, 0xcb, 0xed, 0x0f
    };

    private static final List<int[]> TEST_SPRITES = ImmutableList.of(SPRITE_1, SPRITE_2);

    @Test
    public void test_writes_fire_pattern_modified_events_for_correct_sprites() {
        final AtomicInteger lastModified = new AtomicInteger(-1);
        doSpritePatternChangeTest(
                vram -> {
                    for (int address = 0x0000; address < 0x1000; address++) {
                        vram.setByte(address, 0xff);
                        int expectedAttribute = address / 16;
                        assertEquals(String.format("Expected write to 0x%04x to fire change on attribute %d",
                                address, expectedAttribute),
                                expectedAttribute, lastModified.get());
                    }
                },
                lastModified::set
        );
    }

    @Test
    public void test_writes_do_not_fire_attribute_modified_events() {
        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpriteAttributesModified(int spriteIndex) {
                fail();
            }

            @Override
            public void onSpritePatternModified(int patternIndex) {
                // Do nothing
            }
        };

        VramModule vram = new VramModule();
        vram.register(listener);
        IntStream.range(0x0000, 0x1000).forEach(addr -> vram.setByte(addr, 0xff));
    }

    @Test
    public void test_all_writes_fire_pattern_modified_events() {
        AtomicInteger numEvents = new AtomicInteger(0);
        doSpritePatternChangeTest(
                vram -> {
                    for (int val = 0xff; val >= 0; val--) {
                        vram.setByte(0x00, val);
                    }
                },
                changedPattern -> numEvents.incrementAndGet()
        );
        assertEquals(256, numEvents.get());
    }

    @Test
    public void test_initial_zero_write_does_not_fire_pattern_modified() {
        doSpritePatternChangeTest(
                vram -> vram.setByte(0x00, 0x00),
                changedPattern -> fail()
        );
    }

    @Test
    public void test_repeated_writes_only_fire_pattern_modified_once() {
        AtomicBoolean alreadyFired = new AtomicBoolean(false);
        doSpritePatternChangeTest(
                vram -> IntStream.range(0, 1000).forEach(x -> vram.setByte(0x00, 0xff)),
                changedPattern ->  {
                    if (alreadyFired.getAndSet(true)) {
                        fail();
                    }
                }
        );
    }

    @Test
    public void test_get_pattern_bytes() {
        assertPatternReadHitsAddress(0, 0x0000);
        assertPatternReadHitsAddress(1, 0x0010);
        assertPatternReadHitsAddress(2, 0x0020);
        assertPatternReadHitsAddress(5, 0x0050);
        assertPatternReadHitsAddress(11, 0x00b0);
        assertPatternReadHitsAddress(200, 0x0c80);
        assertPatternReadHitsAddress(255, 0x0ff0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_pattern_bytes_fails_for_index_negative_one() {
        new VramModule().getPatternBytes(-1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_pattern_bytes_fails_for_index_negative_100() {
        new VramModule().getPatternBytes(-100);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_pattern_bytes_fails_for_index_256() {
        new VramModule().getPatternBytes(256);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_pattern_bytes_fails_for_index_257() {
        new VramModule().getPatternBytes(257);
    }

    private void doSpritePatternChangeTest(Consumer<VramModule> test, Consumer<Integer> onPatternChange) {
        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpriteAttributesModified(int spriteIndex) {
                // Do nothing
            }

            @Override
            public void onSpritePatternModified(int patternIndex) {
                onPatternChange.accept(patternIndex);
            }
        };

        VramModule vram = new VramModule();
        vram.register(listener);
        test.accept(vram);
    }

    private void assertPatternReadHitsAddress(int patternIndex, int address) {
        VramModule vram = new VramModule();
        TEST_SPRITES.forEach(patterns -> {
            for (int idx = 0; idx < 16; idx++) {
                vram.setByte(address + idx, patterns[idx]);
            }
            assertArrayEquals(patterns, vram.getPatternBytes(patternIndex));
        });
    }
}
