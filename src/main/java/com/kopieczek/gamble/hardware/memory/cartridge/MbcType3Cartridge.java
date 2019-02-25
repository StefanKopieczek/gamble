package com.kopieczek.gamble.hardware.memory.cartridge;

import com.google.common.base.Preconditions;
import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.RamModule;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MbcType3Cartridge extends GameCartridge {
    private BankedRom romBank;
    private BankedRam ramBank;

    public MbcType3Cartridge(int[] data) {
        super(data);
    }

    @Override
    protected MemoryModule buildRom0(int[] data) {
        return new MemoryModule(Mmu.ROM_0_SIZE) {
            @Override
            public int readByte(int address){
                return data[address];
            }

            @Override
            protected void setByteDirect(int address, int value) {
                if (address < 0x2000) {
                    ramBank.setEnabled((value & 0x0a) > 0);
                } else {
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
        ramBank = new BankedRam();
        return ramBank;
    }

    @Override
    public int getRamSize() {
        return BankedRam.NUM_BANKS * Mmu.EXT_RAM_SIZE;
    }

    @Override
    public void importRamData(int[] data) {
        ramBank.importData(data);
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
            if (address == 0x0000) {
                ramBank.setBank(value);
            }
        }

        private void switchBank(int bank) {
            this.bank = bank;
        }
    }

    private class BankedRam extends MemoryModule {
        private static final int NUM_BANKS = 8;
        private boolean isEnabled = false;
        private int bankIdx = 0;
        private final List<RamModule> ramBanks = new ArrayList<>();

        private BankedRam() {
            super(Mmu.EXT_RAM_SIZE);
            initRam();
        }

        private void initRam() {
            for (int idx = 0; idx < NUM_BANKS; idx++) {
                ramBanks.add(new RamModule(Mmu.EXT_RAM_SIZE));
            }
        }

        public void setEnabled(boolean isEnabled) {
            this.isEnabled = isEnabled;
        }

        @Override
        public int readByte(int address) {
            if (isEnabled) {
                return ramBanks.get(bankIdx).readByte(address);
            } else {
                return 0xff;
            }
        }

        @Override
        protected void setByteDirect(int address, int value) {
            if (isEnabled) {
                ramBanks.get(bankIdx).setByte(address, value);
                int byteIndex = bankIdx * Mmu.EXT_RAM_SIZE + address;
                ramListeners.forEach(l -> {
                    l.onRamChanged(byteIndex, value);
                });
            }
        }

        public void setBank(int bankIdx) {
            if (bankIdx < NUM_BANKS) {
                this.bankIdx = bankIdx;
            }
        }

        void importData(int[] data) {
            Preconditions.checkArgument(data.length == Mmu.EXT_RAM_SIZE * NUM_BANKS);
            for (int idx = 0; idx < NUM_BANKS; idx++) {
                int start = Mmu.EXT_RAM_SIZE * idx;
                int end = start + Mmu.EXT_RAM_SIZE;
                ramBanks.get(idx).importData(Arrays.copyOfRange(data, start, end));
            }
        }
    }
}
