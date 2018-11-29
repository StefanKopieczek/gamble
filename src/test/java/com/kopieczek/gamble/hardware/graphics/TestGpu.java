package com.kopieczek.gamble.hardware.graphics;

import com.kopieczek.gamble.hardware.memory.GraphicsAccessController;
import com.kopieczek.gamble.hardware.memory.InterruptLine;
import com.kopieczek.gamble.hardware.memory.Io;
import com.kopieczek.gamble.hardware.memory.Memory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class TestGpu {
    private static final int[] SHORT_SPRITE_1 = new int[] {
            0x12, 0x34, 0x56, 0x78, 0x9a, 0xbc, 0xde, 0xf0,
            0x10, 0x2f, 0x3e, 0x4d, 0x5c, 0x6b, 0x7a, 0x89
    };

    private static final int[] SHORT_SPRITE_2 = new int[] {
            0x01, 0xf2, 0xe3, 0xd4, 0xc5, 0xb6, 0xa7, 0x98,
            0x20, 0x43, 0x65, 0x87, 0xa9, 0xcb, 0xed, 0x0f
    };

    private static final int[] LONG_SPRITE_1 = new int[] {
            0x01, 0xf2, 0xe3, 0xd4, 0xc5, 0xb6, 0xa7, 0x98,
            0x20, 0x43, 0x65, 0x87, 0xa9, 0xcb, 0xed, 0x0f,
            0x12, 0x34, 0x56, 0x78, 0x9a, 0xbc, 0xde, 0xf0,
            0x10, 0x2f, 0x3e, 0x4d, 0x5c, 0x6b, 0x7a, 0x89
    };

    private static final int[] LONG_SPRITE_2 = new int[] {
            0x20, 0x43, 0x65, 0x87, 0xa9, 0xcb, 0xed, 0x0f,
            0x10, 0x2f, 0x3e, 0x4d, 0x5c, 0x6b, 0x7a, 0x89,
            0x01, 0xf2, 0xe3, 0xd4, 0xc5, 0xb6, 0xa7, 0x98,
            0x12, 0x34, 0x56, 0x78, 0x9a, 0xbc, 0xde, 0xf0,
    };

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
        when(io.getSpritePatternStartAddress()).thenReturn(0xfe00);
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

    @Test
    public void test_get_short_sprite_data() {
        when(io.getSpriteHeight()).thenReturn(8);

        mockByteRange(0x8000, SHORT_SPRITE_1);
        assertArrayEquals(SHORT_SPRITE_1, getTestGpu().getSpriteData(0));

        mockByteRange(0x8010, SHORT_SPRITE_2);
        assertArrayEquals(SHORT_SPRITE_2, getTestGpu().getSpriteData(1));

        mockByteRange(0x8040, SHORT_SPRITE_2);
        assertArrayEquals(SHORT_SPRITE_2, getTestGpu().getSpriteData(4));

        mockByteRange(0x8760, SHORT_SPRITE_1);
        assertArrayEquals(SHORT_SPRITE_1, getTestGpu().getSpriteData(118));
    }

    @Test
    public void test_get_tall_sprite_data() {
        when(io.getSpriteHeight()).thenReturn(16);

        mockByteRange(0x8000, LONG_SPRITE_2);
        assertArrayEquals(LONG_SPRITE_2, getTestGpu().getSpriteData(0));

        mockByteRange(0x8020, LONG_SPRITE_1);
        assertArrayEquals(LONG_SPRITE_1, getTestGpu().getSpriteData(1));

        mockByteRange(0x8080, LONG_SPRITE_2);
        assertArrayEquals(LONG_SPRITE_2, getTestGpu().getSpriteData(4));

        mockByteRange(0x8f00, LONG_SPRITE_1);
        assertArrayEquals(LONG_SPRITE_1, getTestGpu().getSpriteData(120));
    }

    @Test
    public void test_get_sprite_pattern_address_for_short_sprites() {
        when(io.getSpriteHeight()).thenReturn(8);
        Gpu gpu = getTestGpu();
        for (int index = 0; index < 40; index++) {
            assertEquals("Unexpected pattern address for index " + index,
                0xfe00 + 4 * index,
                         gpu.getSpritePatternAddress(index));
        }
    }

    @Test
    public void test_get_sprite_pattern_address_for_long_sprites() {
        when(io.getSpriteHeight()).thenReturn(16);
        Gpu gpu = getTestGpu();
        for (int index = 0; index < 40; index++) {
            assertEquals("Unexpected pattern address for index " + index,
                    0xfe00 + 4 * index, // Same as for short sprites!
                    gpu.getSpritePatternAddress(index));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_get_sprite_pattern_address_fails_for_negative_1() {
        getTestGpu().getSpritePatternAddress(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_get_sprite_pattern_address_fails_for_negative_17() {
        getTestGpu().getSpritePatternAddress(-17);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_get_sprite_pattern_address_fails_for_40() {
        getTestGpu().getSpritePatternAddress(40);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_get_sprite_pattern_address_fails_for_41() {
        getTestGpu().getSpritePatternAddress(41);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_get_sprite_pattern_address_fails_for_101() {
        getTestGpu().getSpritePatternAddress(101);
    }

    private void mockByteRange(int address, int... data) {
        for (int b : data) {
            when(memory.readByte(address)).thenReturn(b);
            address++;
        }
    }

    private Gpu getTestGpu() {
        return new Gpu(memory, io, interrupts, graphics, mmu.getOam(), mmu.getVram());
    }
}
