package com.kopieczek.gamble.hardware.graphics;

import com.kopieczek.gamble.hardware.memory.GraphicsAccessController;
import com.kopieczek.gamble.hardware.memory.InterruptLine;
import com.kopieczek.gamble.hardware.memory.Io;
import com.kopieczek.gamble.hardware.memory.Memory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class TestGpu {
    @Mock
    Memory memory;

    @Mock
    Io io;

    @Mock
    InterruptLine interrupts;

    @Mock
    GraphicsAccessController graphics;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(io.getSpriteDataStartAddress()).thenReturn(0x8000);
    }

    @Test
    public void test_get_sprite_data_address_for_short_sprites() {
        when(io.getSpriteHeight()).thenReturn(8);
        Gpu gpu = getTestGpu();
        assertEquals(0x8000, gpu.getSpriteDataAddress(0));
        assertEquals(0x8010, gpu.getSpriteDataAddress(1));
        assertEquals(0x8020, gpu.getSpriteDataAddress(2));
        assertEquals(0x8080, gpu.getSpriteDataAddress(8));
        assertEquals(0x8090, gpu.getSpriteDataAddress(9));
        assertEquals(0x8100, gpu.getSpriteDataAddress(16));
        assertEquals(0x8ff0, gpu.getSpriteDataAddress(255));
    }

    @Test
    public void test_get_sprite_data_address_for_tall_sprites() {
        when(io.getSpriteHeight()).thenReturn(16);
        Gpu gpu = getTestGpu();
        assertEquals(0x8000, gpu.getSpriteDataAddress(0));
        assertEquals(0x8020, gpu.getSpriteDataAddress(1));
        assertEquals(0x8040, gpu.getSpriteDataAddress(2));
        assertEquals(0x80a0, gpu.getSpriteDataAddress(5));
        assertEquals(0x8100, gpu.getSpriteDataAddress(8));
        assertEquals(0x8120, gpu.getSpriteDataAddress(9));
        assertEquals(0x8200, gpu.getSpriteDataAddress(16));
        assertEquals(0x8fe0, gpu.getSpriteDataAddress(127));
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_sprite_data_address_fails_when_index_is_minus_1() {
        getTestGpu().getSpriteDataAddress(-1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_sprite_data_address_fails_when_index_is_minus_31() {
        getTestGpu().getSpriteDataAddress(-31);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_sprite_data_address_fails_when_index_is_256() {
        getTestGpu().getSpriteDataAddress(256);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_sprite_data_address_fails_when_index_is_1000() {
        getTestGpu().getSpriteDataAddress(1000);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_get_sprite_data_address_fails_for_tall_sprite_when_index_is_128() {
        getTestGpu().getSpriteDataAddress(128);
    }

    private Gpu getTestGpu() {
        return new Gpu(memory, io, interrupts, graphics);
    }
}
