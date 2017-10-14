package com.kopieczek.gamble.memory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestSimpleMemoryModule {
    @Test
    public void test_read_uninitialized_byte_at_start_is_zero() {
        MemoryModule mm = new SimpleMemoryModule();
        assertEquals(0, mm.readByte(0));
    }
}
