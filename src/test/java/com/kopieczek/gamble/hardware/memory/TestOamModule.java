package com.kopieczek.gamble.hardware.memory;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestOamModule {
    @Test
    public void test_writes_fire_pattern_modified_events_for_correct_sprites() {
        final AtomicInteger lastModified = new AtomicInteger(-1);
        doSpritePatternChangeTest(
            oam -> {
                for (int address = 0x00; address < 0xa0; address++) {
                    oam.setByte(address, 0xff);
                    int expectedSprite = address / 4;
                    assertEquals(String.format("Expected write to 0x%02x to fire change on sprite %d",
                                               address, expectedSprite),
                            expectedSprite, lastModified.get());
                }
            },
            lastModified::set
        );
    }

    @Test
    public void test_writes_do_not_fire_attributes_modified_events() {
        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpritePatternModified(int spriteIndex) {
                // Do nothing
            }

            @Override
            public void onSpriteAttributesModified(int patternIndex) {
                fail();
            }
        };

        OamModule oam = new OamModule();
        oam.register(listener);
        IntStream.range(0x00, 0xa0).forEach(addr -> oam.setByte(addr, 0xff));
    }

    @Test
    public void test_all_writes_fire_pattern_modified_events() {
        AtomicInteger numEvents = new AtomicInteger(0);
        doSpritePatternChangeTest(
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
    public void test_initial_zero_write_does_not_fire_pattern_modified() {
        doSpritePatternChangeTest(
            oam -> oam.setByte(0x00, 0x00),
            changedSprite -> fail()
        );
    }

    @Test
    public void test_repeated_writes_only_fire_pattern_modified_once() {
        AtomicBoolean alreadyFired = new AtomicBoolean(false);
        doSpritePatternChangeTest(
            oam -> IntStream.range(0, 1000).forEach(x -> oam.setByte(0x00, 0xff)),
            changedSprite ->  {
                if (alreadyFired.getAndSet(true)) {
                    fail();
                }
            }
        );
    }

    private void doSpritePatternChangeTest(Consumer<OamModule> test, Consumer<Integer> onDataChange) {
        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpritePatternModified(int spriteIndex) {
                onDataChange.accept(spriteIndex);
            }

            @Override
            public void onSpriteAttributesModified(int patternIndex) {
                // Do nothing
            }
        };

        OamModule oam = new OamModule();
        oam.register(listener);
        test.accept(oam);
    }
}
