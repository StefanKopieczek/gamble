package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.hardware.memory.Mmu;

public class EmptyCartridge implements Cartridge {
    private final MemoryModule rom0 = new VoidMemory(Mmu.ROM_0_SIZE);
    private final MemoryModule rom1 = new VoidMemory(Mmu.ROM_1_SIZE);
    private final MemoryModule ram = new VoidMemory(Mmu.EXT_RAM_SIZE);

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

    private static class VoidMemory extends MemoryModule {
        VoidMemory(int size) {
            super(size);
        }

        @Override
        public int readByte(int address) {
            return 0x00;
        }

        @Override
        public void setByteDirect(int address, int value) {
            // Do nothing
        }
    }
}
