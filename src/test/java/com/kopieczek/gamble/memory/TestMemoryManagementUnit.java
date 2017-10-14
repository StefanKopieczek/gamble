package com.kopieczek.gamble.memory;

import org.junit.Test;

public class TestMemoryManagementUnit {
    private static final int BIOS_SIZE = 0x100;
    private static final int ROM_0_SIZE = 0x4000;
    private static final int ROM_1_SIZE = 0x4000;
    private static final int VRAM_SIZE = 0x2000;
    private static final int EXTRAM_SIZE = 0x2000;
    private static final int RAM_SIZE = 0x2000;
    private static final int SPRITES_SIZE = 0xA0;
    private static final int IO_SIZE = 0x80;
    private static final int ZRAM_SIZE = 0x80;

    @Test(expected=IllegalArgumentException.class)
    public void test_invalid_bios_size_rejected() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE - 1),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
    }

    @Test
    public void test_valid_memory_modules_accepted() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
    }
}
