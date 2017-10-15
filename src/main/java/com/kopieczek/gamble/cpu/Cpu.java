package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.MemoryManagementUnit;

public class Cpu {
    private final MemoryManagementUnit mmu;
    private int pc = 0;
    private int cycles = 0;
    private int[] registers;
    private boolean[] flags;

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

        switch (opcode) {
            case 0x00: // NOP
                flags[Flag.ZERO.ordinal()] = false;
            case 0x3c: // INC A
                increment(Register.A);
                break;
            case 0x04: // INC B
                increment(Register.B);
                break;
            case 0x0c: // INC C
                increment(Register.C);
                break;
            case 0x14: // INC D
                increment(Register.D);
                break;
            case 0x1c: // INC E
                increment(Register.E);
                break;
            case 0x24: // INC H
                increment(Register.H);
                break;
            case 0x2c: // INC L
                increment(Register.L);
                break;
            default:
                throw new IllegalArgumentException("Unknown opcode " + Integer.toHexString(opcode));
        }

        pc += 1;
        cycles += 4;
    }

    private void increment(Register r) {
        registers[r.ordinal()]++;
        registers[r.ordinal()] &= 0xff;

        flags[Flag.ZERO.ordinal()] = (registers[r.ordinal()] == 0);
    }

    public int getProgramCounter() {
        return pc;
    }

    public int getCycles() {
        return cycles;
    }

    public int readRegister(Register r) {
        return registers[r.ordinal()];
    }

    public boolean isSet(Flag flag) {
        return (flags[flag.ordinal()]);
    }
}
