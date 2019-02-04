package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.RamModule;
import com.kopieczek.gamble.hardware.memory.RomModule;

import java.util.Arrays;

class MbcType0Cartridge extends GameCartridge {
    MbcType0Cartridge(int[] data) {
        super(data);
    }

    @Override
    protected MemoryModule buildRom0(int[] data) {
        return new RomModule(Arrays.copyOfRange(data, Mmu.ROM_0_START, Mmu.ROM_0_START + Mmu.ROM_0_SIZE));
    }

    @Override
    protected MemoryModule buildRom1(int[] data) {
        return new RomModule(Arrays.copyOfRange(data, Mmu.ROM_1_START, Mmu.ROM_1_START + Mmu.ROM_1_SIZE));
    }

    @Override
    protected MemoryModule buildRam(int[] data) {
        return new RamModule(Mmu.EXT_RAM_SIZE);
    }

    @Override
    public byte[] exportRamData() {
        return new byte[0]; // No RAM to export
    }

    @Override
    public void importRamData(byte[] data) {
        if (data.length > 0) {
            throw new IllegalArgumentException("Unable to import non-empty RAM; this cartridge type has no RAM");
        }
    }
}
