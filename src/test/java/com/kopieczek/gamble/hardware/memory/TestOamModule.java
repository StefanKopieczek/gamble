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
        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpritePatternModified(int patternIndex) {
                fail();
            }

            @Override
            public void onSpriteAttributesModified(int spriteIndex) {
                // Do nothing
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

    private void doSpriteAttributesChangeTest(Consumer<OamModule> test, Consumer<Integer> onAttributesChanged) {
        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpritePatternModified(int patternIndex) {
                // Do nothing
            }

            @Override
            public void onSpriteAttributesModified(int spriteIndex) {
                onAttributesChanged.accept(spriteIndex);
            }
        };

        OamModule oam = new OamModule();
        oam.register(listener);
        test.accept(oam);
    }
}
