package com.kopieczek.gamble.hardware.memory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestMmu {
    private static final int BIOS_SIZE = 0x100;
    private static final int ROM_0_SIZE = 0x4000;
    private static final int ROM_1_SIZE = 0x4000;
    private static final int VRAM_SIZE = 0x2000;
    private static final int EXTRAM_SIZE = 0x2000;
    private static final int RAM_SIZE = 0x2000;
    private static final int SHADOW_RAM_SIZE = 0x1e00; // Shadow RAM shadows all but last 512 bytes of main RAM.
    private static final int SPRITES_SIZE = 0xA0;
    private static final int IO_SIZE = 0x80;
    private static final int ZRAM_SIZE = 0x80;

    private static final int BIOS_START = 0x0;
    private static final int ROM_0_START = 0x0;
    private static final int ROM_1_START = ROM_0_START + ROM_0_SIZE;
    private static final int VRAM_START = ROM_1_START + ROM_1_SIZE;
    private static final int EXTRAM_START = VRAM_START + VRAM_SIZE;
    private static final int RAM_START = EXTRAM_START + EXTRAM_SIZE;
    private static final int SHADOW_RAM_START = RAM_START + RAM_SIZE;
    private static final int SPRITES_START = SHADOW_RAM_START + SHADOW_RAM_SIZE;
    private static final int DEAD_AREA_START = SPRITES_START + SPRITES_SIZE;
    private static final int DEAD_AREA_SIZE = 0x60;
    private static final int IO_START = DEAD_AREA_START + DEAD_AREA_SIZE;
    private static final int ZRAM_START = IO_START + IO_SIZE;

    @Test
    public void test_valid_memory_modules_accepted() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_incorrect_bios_size_rejected() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE - 1),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_incorrect_rom0_size_rejected() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE + 1),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_incorrect_rom1_size_rejected() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE - 100),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_incorrect_vram_size_rejected() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE + 17),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_incorrect_extram_size_rejected() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE - 8),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_incorrect_ram_size_rejected() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE + 16),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_incorrect_sprites_size_rejected() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE - 10),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_incorrect_io_size_rejected() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new DummyIoModule(IO_SIZE + 11),
                new RamModule(ZRAM_SIZE)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_incorrect_zram_size_rejected() {
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE - 99)
        );
    }

    @Test
    public void test_read_from_bios() {
        RamModule bios = new RamModule(BIOS_SIZE);
        Mmu mmu = new Mmu(
                bios,
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, bios, BIOS_START);
    }

    @Test
    public void test_read_from_rom1() {
        RamModule rom1 = new RamModule(ROM_1_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                rom1,
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, rom1, ROM_1_START);
    }

    @Test
    public void test_read_with_bios_switch() {
        RamModule bios = new RamModule(BIOS_SIZE);
        RamModule rom0 = new RamModule(ROM_0_SIZE);
        Mmu mmu = new Mmu(
                bios,
                rom0,
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        bios.setByte(0x00, 0x10);
        bios.setByte(BIOS_SIZE - 1, 0x20);
        rom0.setByte(0x00, 0x30);
        rom0.setByte(BIOS_SIZE - 1, 0x40);

        // BIOS is initially enabled.
        assertEquals(0x10, mmu.readByte(0x00));
        assertEquals(0x20, mmu.readByte(BIOS_SIZE - 1));

        mmu.setBiosEnabled(false);
        assertEquals(0x30, mmu.readByte(0x00));
        assertEquals(0x40, mmu.readByte(BIOS_SIZE - 1));

        mmu.setBiosEnabled(true);
        assertEquals(0x10, mmu.readByte(0x00));
        assertEquals(0x20, mmu.readByte(BIOS_SIZE - 1));
    }

    @Test
    public void test_read_from_rom0() {
        RamModule rom0 = new RamModule(ROM_0_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                rom0,
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        mmu.setBiosEnabled(false);
        testMmuRead(mmu, rom0, ROM_0_START);
    }

    @Test
    public void test_read_from_rom0_with_bios_enabled() {
        RamModule rom0 = new RamModule(ROM_0_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                rom0,
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        rom0.setByte(BIOS_SIZE + 1, 0xff);
        rom0.setByte(ROM_1_START - 1, 0x20);
        assertEquals(0xff, mmu.readByte(BIOS_SIZE + 1));
        assertEquals(0x20, mmu.readByte(ROM_1_START - 1));
    }

    @Test
    public void test_read_from_gpu_vram() {
        RamModule vram = new RamModule(VRAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                vram,
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, vram, VRAM_START);
    }

    @Test
    public void test_read_from_extram() {
        RamModule extram = new RamModule(EXTRAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                extram,
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, extram, EXTRAM_START);
    }

    @Test
    public void test_read_from_ram() {
        RamModule ram = new RamModule(RAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                ram,
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, ram, RAM_START);
    }

    @Test
    public void test_read_from_shadow_ram() {
        RamModule ram = new RamModule(RAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                ram,
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, ram, SHADOW_RAM_START, SHADOW_RAM_SIZE);
    }

    @Test
    public void test_read_from_sprite_area() {
        RamModule sprites = new RamModule(SPRITES_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                sprites,
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, sprites, SPRITES_START);
    }

    @Test
    public void test_read_from_io_area() {
        IoModule io = new DummyIoModule(IO_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                io,
                new RamModule(ZRAM_SIZE)
        );

        testMmuRead(mmu, io, IO_START);
    }

    @Test
    public void test_read_from_zram() {
        RamModule zram = new RamModule(ZRAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                zram
        );

        testMmuRead(mmu, zram, ZRAM_START);
    }

    @Test
    public void test_write_to_bios() {
        RamModule bios = new RamModule(BIOS_SIZE);
        Mmu mmu = new Mmu(
                bios,
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuWrite(mmu, bios, BIOS_START);
    }

    @Test
    public void test_write_to_rom1() {
        RamModule rom1 = new RamModule(ROM_1_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                rom1,
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuWrite(mmu, rom1, ROM_1_START);
    }

    @Test
    public void test_write_with_bios_switch() {
        RamModule bios = new RamModule(BIOS_SIZE);
        RamModule rom0 = new RamModule(ROM_0_SIZE);
        Mmu mmu = new Mmu(
                bios,
                rom0,
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        // BIOS is initially enabled.
        mmu.setByte(0x00, 0x10);
        mmu.setByte(BIOS_SIZE - 1, 0x20);
        assertEquals(0x10, bios.readByte(0x00));
        assertEquals(0x20, bios.readByte(BIOS_SIZE - 1));

        mmu.setBiosEnabled(false);
        mmu.setByte(0x00, 0x30);
        mmu.setByte(BIOS_SIZE - 1, 0x40);
        assertEquals(0x30, rom0.readByte(0x00));
        assertEquals(0x40, rom0.readByte(BIOS_SIZE - 1));

        // Check that the BIOS page isn't modified by switching.
        mmu.setBiosEnabled(true);
        assertEquals(0x10, bios.readByte(0x00));
        assertEquals(0x20, bios.readByte(BIOS_SIZE - 1));

        // Check that the ROM0 page isn't modified by switching.
        mmu.setBiosEnabled(false);
        assertEquals(0x30, rom0.readByte(0x00));
        assertEquals(0x40, rom0.readByte(BIOS_SIZE - 1));
    }

    @Test
    public void test_write_to_rom0() {
        RamModule rom0 = new RamModule(ROM_0_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                rom0,
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        mmu.setBiosEnabled(false);
        testMmuWrite(mmu, rom0, ROM_0_START);
    }

//    @Test
//    public void test_write_to_rom0_with_bios_enabled() {
//        RamModule rom0 = new RamModule(ROM_0_SIZE);
//        Mmu mmu = new Mmu(
//                new RamModule(BIOS_SIZE),
//                rom0,
//                new RamModule(ROM_1_SIZE),
//                new RamModule(VRAM_SIZE),
//                new RamModule(EXTRAM_SIZE),
//                new RamModule(RAM_SIZE),
//                new RamModule(SPRITES_SIZE),
//                new IoModule(),
//                new RamModule(ZRAM_SIZE)
//        );
//
//        rom0.setByte(BIOS_SIZE + 1, 0xff);
//        rom0.setByte(ROM_1_START - 1, 0x20);
//        assertEquals(0xff, mmu.readByte(BIOS_SIZE + 1));
//        assertEquals(0x20, mmu.readByte(ROM_1_START - 1));
//    }

    @Test
    public void test_write_to_gpu_vram() {
        RamModule vram = new RamModule(VRAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                vram,
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuWrite(mmu, vram, VRAM_START);
    }

    @Test
    public void test_write_to_extram() {
        RamModule extram = new RamModule(EXTRAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                extram,
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuWrite(mmu, extram, EXTRAM_START);
    }

    @Test
    public void test_write_to_ram() {
        RamModule ram = new RamModule(RAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                ram,
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuWrite(mmu, ram, RAM_START);
    }

    @Test
    public void test_write_to_shadow_ram() {
        RamModule ram = new RamModule(RAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                ram,
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuWrite(mmu, ram, SHADOW_RAM_START, SHADOW_RAM_SIZE);
    }

    @Test
    public void test_write_to_sprite_area() {
        RamModule sprites = new RamModule(SPRITES_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                sprites,
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );

        testMmuWrite(mmu, sprites, SPRITES_START);
    }

    @Test
    public void test_write_to_io_area() {
        IoModule io = new DummyIoModule(IO_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                io,
                new RamModule(ZRAM_SIZE)
        );

        testMmuWrite(mmu, io, IO_START);
    }

    @Test
    public void test_write_to_zram() {
        RamModule zram = new RamModule(ZRAM_SIZE);
        Mmu mmu = new Mmu(
                new RamModule(BIOS_SIZE),
                new RamModule(ROM_0_SIZE),
                new RamModule(ROM_1_SIZE),
                new RamModule(VRAM_SIZE),
                new RamModule(EXTRAM_SIZE),
                new RamModule(RAM_SIZE),
                new RamModule(SPRITES_SIZE),
                new IoModule(),
                zram
        );

        testMmuWrite(mmu, zram, ZRAM_START);
    }

    private void testMmuRead(Mmu mmu, MemoryModule module, int addressOffset, int maxSize) {
        int start = 0;
        int mid = module.getSizeInBytes() / 2;
        int end = maxSize - 1;
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

    private void testMmuRead(Mmu mmu, MemoryModule module, int addressOffset) {
        testMmuRead(mmu, module, addressOffset, module.getSizeInBytes());
    }

    private void testMmuWrite(Mmu mmu, MemoryModule module, int addressOffset, int maxSize) {
        int start = 0;
        int mid = module.getSizeInBytes() / 2;
        int end = maxSize - 1;
        int startVal = 0x01;
        int midVal = 0xa0;
        int endVal = 0xff;
        mmu.setByte(addressOffset + start, startVal);
        mmu.setByte(addressOffset + mid, midVal);
        mmu.setByte(addressOffset + end, endVal);
        assertEquals(startVal, module.readByte(start));
        assertEquals(midVal, module.readByte(mid));
        assertEquals(endVal, module.readByte(end));
    }

    private void testMmuWrite(Mmu mmu, MemoryModule module, int addressOffset) {
        testMmuWrite(mmu, module, addressOffset, module.getSizeInBytes());
    }

    private static class DummyIoModule extends IoModule {
        // IO has a lot of memory mapped weirdness.
        // For the purpose of MMU testing, delegate to a more simple store
        // so we can make sure the MMU triggers the right reads and writes
        // without dealing with IO complexities.
        private final RamModule delegate;

        DummyIoModule(int size) {
            delegate = new RamModule(size);
        }

        @Override
        public int getSizeInBytes() {
            return delegate.getSizeInBytes();
        }

        @Override
        public int readByte(int address) {
            return delegate.readByte(address);
        }

        @Override
        public void setByte(int address, int value) {
            delegate.setByte(address, value);
        }
    }
}
