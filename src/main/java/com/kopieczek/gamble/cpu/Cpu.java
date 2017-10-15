package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.MemoryManagementUnit;

public class Cpu {
    final MemoryManagementUnit mmu;
    int pc = 0;
    int cycles = 0;
    int[] registers;
    boolean[] flags;

    public Cpu(MemoryManagementUnit mmu) {
        this.mmu = mmu;
        this.registers = new int[Register.values().length];
        this.flags = new boolean[Flag.values().length];
    }

    int readByte(int address) {
        return mmu.readByte(address);
    }

    void setByte(int address, int value) {
        mmu.setByte(address, value);
    }

    public void tick() {
        int opcode = mmu.readByte(pc);

        Operation op = Operation.map.get(opcode);
        if (op != null) {
            op.execute(this);
        } else {
            throw new IllegalArgumentException("Unknown opcode 0x" + Integer.toHexString(opcode));
        }

        pc += 1;
    }

    void increment(Register r) {
        registers[r.ordinal()]++;
        registers[r.ordinal()] &= 0xff;
    }

    public int getProgramCounter() {
        return pc;
    }

    public int getCycles() {
        return cycles;
    }

    public int readByte(Register r) {
        return registers[r.ordinal()];
    }

    public boolean isSet(Flag flag) {
        return (flags[flag.ordinal()]);
    }
}
