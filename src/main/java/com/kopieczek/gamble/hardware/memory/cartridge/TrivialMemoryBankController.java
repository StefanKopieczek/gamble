package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.RamModule;

public class TrivialMemoryBankController implements MemoryBankController {
    private final RamModule ram = new RamModule(Mmu.EXT_RAM_SIZE);

    @Override
    public int mapAddress(int busAddress) {
        return busAddress;
    }

    @Override
    public MemoryModule getRam() {
        return ram;
    }
}