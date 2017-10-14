package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.MemoryManagementUnit;

public class Cpu {
    private final MemoryManagementUnit mmu;
    private int pc = 0;

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
        pc += 1;
    }

    public int getProgramCounter() {
        return pc;
    }
}
