package com.kopieczek.gamble.hardware.memory;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestVramModule {
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
}
