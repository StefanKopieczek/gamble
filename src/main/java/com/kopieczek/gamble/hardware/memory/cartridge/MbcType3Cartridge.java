package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.RamModule;

public class MbcType3Cartridge extends GameCartridge {
    private BankedRom romBank;

    public MbcType3Cartridge(int[] data) {
        super(data);
    }

    @Override
    protected MemoryModule buildRom0(int[] data) {
        return new MemoryModule(Mmu.ROM_0_SIZE) {
            @Override
            public int readByte(int address) {
                return 0;
            }

            @Override
            protected void setByteDirect(int address, int value) {
                if (address >= 0x2000) {
                    value &= 0x7f;
                    value = (value == 0x00) ? 0x01 : value; // Requests for bank 0 must yield bank 1
                    romBank.switchBank(value);
                }
            }
        };
    }

    @Override
    protected MemoryModule buildRom1(int[] data) {
        romBank = new BankedRom(data);
        return romBank;
    }

    @Override
    protected MemoryModule buildRam(int[] data) {
        return new RamModule(Mmu.RAM_SIZE);
    }

    private class BankedRom extends MemoryModule {
        private int[] data;
        private int bank = 1;

        private BankedRom(int[] data) {
            super(Mmu.ROM_1_SIZE);
            this.data = data;
        }

        @Override
        public int readByte(int address) {
            return data[Mmu.ROM_1_SIZE * bank + address];
        }

        @Override
        protected void setByteDirect(int address, int value) {
        }

        private void switchBank(int bank) {
            this.bank = bank;
        }
    }
}
