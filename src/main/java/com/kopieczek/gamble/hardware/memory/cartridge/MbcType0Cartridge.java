package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.RamModule;
import com.kopieczek.gamble.hardware.memory.RomModule;

import java.util.Arrays;

class MbcType0Cartridge implements Cartridge {
    private final RomModule rom0;
    private final RomModule rom1;
    private final RamModule ram;

    MbcType0Cartridge(int[] data) {
        rom0 = new RomModule(Arrays.copyOfRange(data, Mmu.ROM_0_START, Mmu.ROM_0_START + Mmu.ROM_0_SIZE));
        rom1 = new RomModule(Arrays.copyOfRange(data, Mmu.ROM_1_START, Mmu.ROM_1_START + Mmu.ROM_1_SIZE));
        ram = new RamModule(Mmu.EXT_RAM_SIZE);
    }

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
