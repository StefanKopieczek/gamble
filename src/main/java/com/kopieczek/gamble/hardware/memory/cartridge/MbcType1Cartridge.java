package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.RamModule;
import com.kopieczek.gamble.hardware.memory.RomModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

class MbcType1Cartridge extends GameCartridge {
    private static final Logger log = LogManager.getLogger(MbcType1Cartridge.class);
    private final MemoryModule[] romBanks = new MemoryModule[125];
    private final MemoryModule[] ramBanks = new RamModule[4];
    private int ramBankRegister = 0;
    private int romBankRegister = 1; // Don't set this directly; use setRomBankRegister.
    private boolean isRamEnabled = false;
    private BankingMode bankingMode = BankingMode.HIGH_ROM;

    MbcType1Cartridge(int[] data) {
        super(data);
        initRomBanks(data);
        initRamBanks(data);
    }

    private void initRomBanks(int[] data) {
        for (int bankId = 0; bankId < romBanks.length; bankId++) {
            // This calculation relies on the fact that ROM_0_SIZE == ROM_1_SIZE.
            int start = Mmu.ROM_0_START + (Mmu.ROM_0_SIZE * bankId);
            int end = start + Mmu.ROM_0_SIZE;
            try {
                log.debug("Init ROM bank {} between {} and {}", bankId,
                    Integer.toHexString(start), Integer.toHexString(end));
                romBanks[bankId] = new RomModule(Arrays.copyOfRange(data, start, end));
            } catch (ArrayIndexOutOfBoundsException e) {
                log.debug("Creating empty rom for bank {}", bankId);
                romBanks[bankId] = new RomModule(new int[Mmu.ROM_1_SIZE]);
            }
        }
    }

    private void initRamBanks(int[] data) {
        for (int bankId = 0; bankId < ramBanks.length; bankId++) {
            ramBanks[bankId] = new RamModule(Mmu.EXT_RAM_SIZE);
        }
    }

    @Override
    protected MemoryModule buildRom0(int[] data) {
        // Rom 0 is fixed at bank 0, and cannot be modified.
        return new MemoryModule(Mmu.ROM_0_SIZE) {
            @Override
            public int readByte(int address) {
                return romBanks[0].readByte(address);
            }

            @Override
            protected void setByteDirect(int address, int value) {
                if (address < 0x2000) {
                    setRamEnabled((value & 0x0f) == 0x0a);
                } else {
                    setRomBankRegister((romBankRegister & 0xe0) + (value & 0x1f));
                }
            }
        };
    }

    @Override
    protected MemoryModule buildRom1(int[] data) {
        return new MemoryModule(Mmu.ROM_1_SIZE) {
            @Override
            public int readByte(int address) {
                return getRomBank().readByte(address);
            }

            @Override
            protected void setByteDirect(int address, int value) {
                // 'Writes' to ROM allow the game code to modify the state of the MBC.
                if (address < 0x2000) {
                    int bankBits = 0x03 & value;
                    if (bankingMode == BankingMode.HIGH_ROM) {
                        setRomBankRegister((bankBits << 5) + (romBankRegister & 0x1f));
                    } else {
                        ramBankRegister = bankBits;
                    }
                } else {
                    bankingMode = ((value & 0x01) == 0) ? BankingMode.HIGH_ROM : BankingMode.RAM;
                }
            }
        };
    }

    @Override
    protected MemoryModule buildRam(int[] data) {
        return new MemoryModule(Mmu.EXT_RAM_SIZE) {
            @Override
            public int readByte(int address) {
                if (isRamEnabled) {
                    return ramBanks[getRamBank()].readByte(address);
                } else {
                    log.warn("Program tried to read from extram while it was disabled");
                    return 0xff;
                }
            }

            @Override
            protected void setByteDirect(int address, int value) {
                if (isRamEnabled) {
                    int bankIdx = getRamBank();
                    ramBanks[bankIdx].setByte(address, value);
                    int byteIndex = bankIdx * Mmu.EXT_RAM_SIZE + address;
                    ramListeners.forEach(l -> {
                        l.onRamChanged(byteIndex, value);
                    });
                } else {
                    log.warn("Program tried to write to extram while it was disabled");
                }
            }
        };
    }

    private MemoryModule getRomBank() {
        if (bankingMode == BankingMode.HIGH_ROM) {
            return romBanks[romBankRegister & 0x7f];
        } else {
            return romBanks[romBankRegister & 0x1f];
        }
    }

    private int getRamBank() {
        if (bankingMode == BankingMode.RAM) {
            return ramBankRegister & 0x03;
        } else {
            return 0;
        }
    }

    private void setRamEnabled(boolean isEnabled) {
        isRamEnabled = isEnabled;
    }

    private void setRomBankRegister(int bankId) {
        if ((bankId & 0x1f) == 0x00) {
            // Weird hardware restriction; can never select a bank where the last five bits of the ID are zero,
            // and instead we roll on to the next bank.
            bankId += 1;
        }

        romBankRegister = bankId;
    }

    @Override
    public int getRamSize() {
        return ramBanks.length * Mmu.EXT_RAM_SIZE;
    }

    @Override
    public void importRamData(int[] data) {
        throw new IllegalStateException("Not yet implemented");
    }

    @Override
    public void addExtRamListener(ExtRamListener listener) {

    }

    private enum BankingMode {
        HIGH_ROM,
        RAM
    }
}
