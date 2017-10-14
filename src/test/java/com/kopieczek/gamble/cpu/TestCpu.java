package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.MemoryManagementUnit;
import com.kopieczek.gamble.memory.SimpleMemoryModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCpu {
    @Test
    public void test_simple_read() {
        MemoryManagementUnit mmu = buildMmu();
        mmu.setByte(0xdead, 0xf0);
        Cpu cpu = new Cpu(mmu);
        assertEquals(0xf0, cpu.readByte(0xdead));
    }

    @Test
    public void test_simple_write() {
        MemoryManagementUnit mmu = buildMmu();
        Cpu cpu = new Cpu(mmu);
        cpu.setByte(0xdead, 0xf0);
        assertEquals(0xf0, mmu.readByte(0xdead));
    }

    private static MemoryManagementUnit buildMmu() {
        return new MemoryManagementUnit(
                new SimpleMemoryModule(MemoryManagementUnit.BIOS_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.ROM_0_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.ROM_1_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.VRAM_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.EXT_RAM_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.RAM_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.SPRITES_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.IO_AREA_SIZE),
                new SimpleMemoryModule(MemoryManagementUnit.ZRAM_SIZE)
        );
    }
}
