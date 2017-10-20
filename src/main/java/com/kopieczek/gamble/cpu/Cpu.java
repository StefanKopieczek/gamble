package com.kopieczek.gamble.cpu;

import com.google.common.collect.ImmutableMap;
import com.kopieczek.gamble.memory.IndirectAddress;
import com.kopieczek.gamble.memory.MemoryManagementUnit;

import java.util.Map;

public class Cpu {
    final MemoryManagementUnit mmu;
    static final Map<Integer, Operation> operations = loadOperations();
    int pc = 0;
    int cycles = 0;
    int[] registers;
    boolean[] flags;

    public Cpu(MemoryManagementUnit mmu) {
        this.mmu = mmu;
        this.registers = new int[Register.values().length];
        this.flags = new boolean[Flag.values().length];
    }

    public int readByte(int address) {
        return mmu.readByte(address);
    }

    public void setByte(int address, int value) {
        mmu.setByte(address, value);
    }

    public void tick() {
        int opcode = mmu.readByte(pc);

        Operation op = operations.get(opcode);
        if (op != null) {
            cycles += op.apply(this);
            pc += 1;
        } else {
            throw new IllegalArgumentException("Unknown opcode 0x" + Integer.toHexString(opcode));
        }
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

    public void set(Register r, int value) {
        registers[r.ordinal()] = value;
    }

    public boolean isSet(Flag flag) {
        return (flags[flag.ordinal()]);
    }

    void set(Flag flag, boolean state) {
        flags[flag.ordinal()] = state;
    }

    private static Map<Integer, Operation> loadOperations() {
        ImmutableMap.Builder<Integer, Operation> m = ImmutableMap.builder();
        m.put(0x00, Operations.nop());
        m.put(0x06, Operations.loadValueTo(Register.B));
        m.put(0x0e, Operations.loadValueTo(Register.C));
        m.put(0x16, Operations.loadValueTo(Register.D));
        m.put(0x1e, Operations.loadValueTo(Register.E));
        m.put(0x26, Operations.loadValueTo(Register.H));
        m.put(0x2e, Operations.loadValueTo(Register.L));
        m.put(0x7f, Operations.nop());
        m.put(0x78, Operations.copyValue(Register.B, Register.A));
        m.put(0x79, Operations.copyValue(Register.C, Register.A));
        m.put(0x7a, Operations.copyValue(Register.D, Register.A));
        m.put(0x7b, Operations.copyValue(Register.E, Register.A));
        m.put(0x7c, Operations.copyValue(Register.H, Register.A));
        m.put(0x7d, Operations.copyValue(Register.L, Register.A));
        m.put(0x7e, Operations.copyValue(IndirectAddress.from(Register.H, Register.L), Register.A));
        m.put(0x3c, Operations.increment(Register.A));
        m.put(0x04, Operations.increment(Register.B));
        m.put(0x0c, Operations.increment(Register.C));
        m.put(0x14, Operations.increment(Register.D));
        m.put(0x1c, Operations.increment(Register.E));
        m.put(0x24, Operations.increment(Register.H));
        m.put(0x2c, Operations.increment(Register.L));
        m.put(0x34, Operations.increment(IndirectAddress.from(Register.H, Register.L)));
        return m.build();
    }
}
