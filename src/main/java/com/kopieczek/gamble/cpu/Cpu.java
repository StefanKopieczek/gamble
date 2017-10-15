package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.MemoryManagementUnit;

public class Cpu {
    private final MemoryManagementUnit mmu;
    private int pc = 0;
    private int cycles = 0;
    private int registerA = 0;
    private int registerB = 0;

    public Cpu(MemoryManagementUnit mmu) {
        this.mmu = mmu;
    }

    int readByte(int address) {
        return mmu.readByte(address);
    }

    void setByte(int address, int value) {
        mmu.setByte(address, value);
    }

    public void tick() {
        int opcode = mmu.readByte(pc);

        if (opcode == 0x3c) {
            registerA++;
        } else if (opcode == 0x04) {
            registerB++;
        }

        pc += 1;
        cycles += 4;
    }

    public int getProgramCounter() {
        return pc;
    }

    public int getCycles() {
        return cycles;
    }

    public int readRegister(Register r) {
        switch (r) {
            case A:
                return registerA;
            case B:
                return registerB;
            default:
                throw new IllegalArgumentException("Unknown register " + r);
        }
    }
}
