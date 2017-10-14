package com.kopieczek.gamble.memory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    private static final int BIOS_START = 0x0;
    private static final int ROM_0_START = 0x0;
    private static final int ROM_1_START = ROM_0_START + ROM_0_SIZE;

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

    @Test(expected=IllegalArgumentException.class)
    public void test_incorrect_bios_size_rejected() {
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

    @Test(expected=IllegalArgumentException.class)
    public void test_incorrect_rom0_size_rejected() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE + 1),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_incorrect_rom1_size_rejected() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE - 100),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_incorrect_vram_size_rejected() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE + 17),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_incorrect_extram_size_rejected() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE - 8),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_incorrect_ram_size_rejected() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE + 16),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_incorrect_sprites_size_rejected() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE - 10),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_incorrect_io_size_rejected() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE + 11),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_incorrect_zram_size_rejected() {
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE - 99)
        );
    }

    @Test
    public void test_read_from_bios() {
        SimpleMemoryModule bios = new SimpleMemoryModule(BIOS_SIZE);
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                bios,
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, bios, ROM_0_START);
    }

    @Test
    public void test_read_from_rom1() {
        SimpleMemoryModule rom1 = new SimpleMemoryModule(ROM_1_SIZE);
        MemoryManagementUnit mmu = new MemoryManagementUnit(
                new SimpleMemoryModule(BIOS_SIZE),
                new SimpleMemoryModule(ROM_0_SIZE),
                rom1,
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXTRAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new SimpleMemoryModule(IO_SIZE),
                new SimpleMemoryModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, rom1, ROM_1_START);
    }

    private void testMmuRead(MemoryManagementUnit mmu, MemoryModule module, int addressOffset) {
        int start = 0;
        int mid = module.getSizeInBytes() / 2;
        int end = module.getSizeInBytes() - 1;
        int startVal = 0x01;
        int midVal = 0xa0;
        int endVal = 0xff;
        module.setByte(start, startVal);
        module.setByte(mid, midVal);
        module.setByte(end, endVal);
        assertEquals(startVal, mmu.readByte(addressOffset + start));
        assertEquals(midVal, mmu.readByte(addressOffset + mid));
        assertEquals(endVal, mmu.readByte(addressOffset + end));
    }
}
