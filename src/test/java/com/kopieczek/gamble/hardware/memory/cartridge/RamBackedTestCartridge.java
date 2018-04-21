package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.SimpleRamModule;

public class RamBackedTestCartridge implements Cartridge {
    private final MemoryModule rom0 = new SimpleRamModule(Mmu.ROM_0_SIZE);
    private final MemoryModule rom1 = new SimpleRamModule(Mmu.ROM_1_SIZE);
    private final MemoryModule ram  = new SimpleRamModule(Mmu.EXT_RAM_SIZE);

    @Override
    public MemoryModule getRom0() {
        return rom0;
    }

    @Override
    public MemoryModule getRom1() {
        return rom1;
    }

    @Override
    public MemoryModule getRam() {
        return ram;
    }
}
