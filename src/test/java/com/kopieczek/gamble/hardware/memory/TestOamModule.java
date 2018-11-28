package com.kopieczek.gamble.hardware.memory;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestOamModule {
    @Test
    public void test_writes_fire_data_modified_events_for_correct_sprites() {
        final AtomicInteger lastModified = new AtomicInteger(-1);

        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpriteDataModified(int spriteIndex) {
                lastModified.set(spriteIndex);
            }

            @Override
            public void onSpritePatternModified(int patternIndex) {
                // Do nothing
            }
        };

        OamModule oam = new OamModule();
        oam.register(listener);

        for (int address = 0x00; address < 0xa0; address++) {
            oam.setByte(address, 0xff);
            int expectedSprite = address / 4;
            assertEquals(String.format("Expected write to 0x%02x to fire change on sprite %d", address, expectedSprite),
                    expectedSprite, lastModified.get());
        }
    }

    @Test
    public void test_writes_do_not_fire_pattern_modified_events() {
        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpriteDataModified(int spriteIndex) {
                // Do nothing
            }

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
    public void test_all_writes_fire_pattern_modified_events() {
        AtomicInteger numEvents = new AtomicInteger(0);
        OamModule oam = new OamModule();
        oam.setByte(0x01, 0x01);

        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpriteDataModified(int spriteIndex) {
                numEvents.incrementAndGet();
            }

            @Override
            public void onSpritePatternModified(int patternIndex) {
                // Do nothing
            }
        };

        oam.register(listener);
        IntStream.range(0, 256).forEach(val -> oam.setByte(0x01, val));
        assertEquals(256, numEvents.get());
    }

    @Test
    public void test_initial_zero_write_does_not_fire_data_modified() {
        SpriteChangeListener listener = new SpriteChangeListener() {
            @Override
            public void onSpriteDataModified(int spriteIndex) {
                fail();
            }

            @Override
            public void onSpritePatternModified(int patternIndex) {
            }
        };

        OamModule oam = new OamModule();
        oam.register(listener);
        oam.setByte(0x00, 0x00);
    }

    @Test
    public void test_repeated_writes_only_fire_data_modified_once() {
        SpriteChangeListener listener = new SpriteChangeListener() {
            private boolean alreadyFired = false;

            @Override
            public void onSpriteDataModified(int spriteIndex) {
                if (alreadyFired) {
                    fail();
                } else {
                    alreadyFired = true;
                }
            }

            @Override
            public void onSpritePatternModified(int patternIndex) {
                // Do nothing
            }
        };

        OamModule oam = new OamModule();
        oam.register(listener);
        IntStream.range(0, 1000).forEach(x -> oam.setByte(0x00, 0xff));
    }
}
