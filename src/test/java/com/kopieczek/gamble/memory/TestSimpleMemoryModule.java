package com.kopieczek.gamble.memory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestSimpleMemoryModule {
    @Test
    public void test_read_uninitialized_byte_at_start_is_zero() {
        MemoryModule mm = new SimpleMemoryModule();
        assertEquals(0x0, mm.readByte(0x0));
    }

    @Test
    public void test_set_and_read_byte_at_start() {
        MemoryModule mm = new SimpleMemoryModule();
        mm.setByte(0x0, 0xff);
        assertEquals(0xff, mm.readByte(0x0));
    }

    @Test
    public void test_read_only_affects_one_location() {
        MemoryModule mm = new SimpleMemoryModule();
        mm.setByte(0x0, 0xff);
        assertEquals(0x0, mm.readByte(0x1));
    }

    @Test
    public void test_set_arbitrary_byte_values() {
        MemoryModule mm = new SimpleMemoryModule();
        mm.setByte(0, 0xab);
        assertEquals(0xab, mm.readByte(0x0));
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_cannot_read_negative_memory() {
        MemoryModule mm = new SimpleMemoryModule();
        mm.readByte(-0x01);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_cannot_write_negative_memory() {
        MemoryModule mm = new SimpleMemoryModule();
        mm.setByte(-0x01, 0xff);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_cannot_read_beyond_max_address() {
        MemoryModule mm = new SimpleMemoryModule(0x2);
        mm.readByte(0x2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_cannot_read_beyond_max_address_2() {
        MemoryModule mm = new SimpleMemoryModule(0x2);
        mm.readByte(0x3);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_cannot_write_beyond_max_address() {
        MemoryModule mm = new SimpleMemoryModule(0x2);
        mm.setByte(0x2, 0xff);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_cannot_write_beyond_max_address_2() {
        MemoryModule mm = new SimpleMemoryModule(0x2);
        mm.setByte(0x3, 0xff);
    }

    @Test()
    public void test_can_read_maximum_addressable_memory() {
        MemoryModule mm = new SimpleMemoryModule(0x2);
        assertEquals(0x0, mm.readByte(0x1));
    }

    @Test()
    public void test_can_write_maximum_addressable_memory() {
        MemoryModule mm = new SimpleMemoryModule(0x2);
        mm.setByte(0x1, 0xfe);
        assertEquals(0xfe, mm.readByte(0x1));
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_cannot_write_negative_value() {
        MemoryModule mm = new SimpleMemoryModule();
        mm.setByte(0x0, -0x01);
    }
}
