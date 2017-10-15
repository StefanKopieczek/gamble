package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.MemoryManagementUnit;

public class Cpu {
    private final MemoryManagementUnit mmu;
    private int pc = 0;
    private int cycles = 0;
    private int[] registers;

    public Cpu(MemoryManagementUnit mmu) {
        this.mmu = mmu;
        this.registers = new int[Register.values().length];
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
                break;
            case 0x3c: // INC A
                registers[Register.A.ordinal()]++;
                break;
            case 0x04: // INC B
                registers[Register.B.ordinal()]++;
                break;
            case 0x0c: // INC C
                registers[Register.C.ordinal()]++;
                break;
            case 0x14: // INC D
                registers[Register.D.ordinal()]++;
                break;
            case 0x1c: // INC E
                registers[Register.E.ordinal()]++;
                break;
            case 0x24: // INC H
                registers[Register.H.ordinal()]++;
                break;
            case 0x2c: // INC L
                registers[Register.L.ordinal()]++;
                break;
            default:
                throw new IllegalArgumentException("Unknown opcode " + Integer.toHexString(opcode));
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
        return registers[r.ordinal()];
    }
}
