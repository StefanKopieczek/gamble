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

    public int readByte(Register r) {
        return registers[r.ordinal()];
    }

    public int readByte(IndirectAddress address) {
        return readByte((readByte(address.left) << 8) + readByte(address.right));
    }

    public void setByte(int address, int value) {
        mmu.setByte(address, value);
    }

    public void set(Register r, int value) {
        registers[r.ordinal()] = value;
    }

    public void set(Register to, IndirectAddress from) {
        int value = readByte(from);
        set(to, value);
    }

    public void setByte(IndirectAddress to, Register from) {
        setByte(to, readByte(from));
    }

    public void setByte(IndirectAddress address, int value) {
        setByte((readByte(address.left) << 8) + readByte(address.right), value);
    }

    public int resolveAddress(IndirectAddress address) {
        return (readByte(address.left) << 8) + readByte(address.right);
    }

    int readNextArg() {
        pc += 1;
        return readByte(pc);
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

    public boolean isSet(Flag flag) {
        return (flags[flag.ordinal()]);
    }

    void set(Flag flag, boolean state) {
        flags[flag.ordinal()] = state;
    }

    private static Map<Integer, Operation> loadOperations() {
        ImmutableMap.Builder<Integer, Operation> m = ImmutableMap.builder();
        m.put(0x00, Operations.nop());
        m.put(0x3e, Operations.loadValueTo(Register.A));
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
        m.put(0x7e, Operations.copyValue(Register.HL, Register.A));
        m.put(0x40, Operations.copyValue(Register.B, Register.B));
        m.put(0x41, Operations.copyValue(Register.C, Register.B));
        m.put(0x42, Operations.copyValue(Register.D, Register.B));
        m.put(0x43, Operations.copyValue(Register.E, Register.B));
        m.put(0x44, Operations.copyValue(Register.H, Register.B));
        m.put(0x45, Operations.copyValue(Register.L, Register.B));
        m.put(0x46, Operations.copyValue(Register.HL, Register.B));
        m.put(0x48, Operations.copyValue(Register.B, Register.C));
        m.put(0x49, Operations.copyValue(Register.C, Register.C));
        m.put(0x4a, Operations.copyValue(Register.D, Register.C));
        m.put(0x4b, Operations.copyValue(Register.E, Register.C));
        m.put(0x4c, Operations.copyValue(Register.H, Register.C));
        m.put(0x4d, Operations.copyValue(Register.L, Register.C));
        m.put(0x4e, Operations.copyValue(Register.HL, Register.C));
        m.put(0x50, Operations.copyValue(Register.B, Register.D));
        m.put(0x51, Operations.copyValue(Register.C, Register.D));
        m.put(0x52, Operations.copyValue(Register.D, Register.D));
        m.put(0x53, Operations.copyValue(Register.E, Register.D));
        m.put(0x54, Operations.copyValue(Register.H, Register.D));
        m.put(0x55, Operations.copyValue(Register.L, Register.D));
        m.put(0x56, Operations.copyValue(Register.HL, Register.D));
        m.put(0x58, Operations.copyValue(Register.B, Register.E));
        m.put(0x59, Operations.copyValue(Register.C, Register.E));
        m.put(0x5a, Operations.copyValue(Register.D, Register.E));
        m.put(0x5b, Operations.copyValue(Register.E, Register.E));
        m.put(0x5c, Operations.copyValue(Register.H, Register.E));
        m.put(0x5d, Operations.copyValue(Register.L, Register.E));
        m.put(0x5e, Operations.copyValue(Register.HL, Register.E));
        m.put(0x60, Operations.copyValue(Register.B, Register.H));
        m.put(0x61, Operations.copyValue(Register.C, Register.H));
        m.put(0x62, Operations.copyValue(Register.D, Register.H));
        m.put(0x63, Operations.copyValue(Register.E, Register.H));
        m.put(0x64, Operations.copyValue(Register.H, Register.H));
        m.put(0x65, Operations.copyValue(Register.L, Register.H));
        m.put(0x66, Operations.copyValue(Register.HL, Register.H));
        m.put(0x68, Operations.copyValue(Register.B, Register.L));
        m.put(0x69, Operations.copyValue(Register.C, Register.L));
        m.put(0x6a, Operations.copyValue(Register.D, Register.L));
        m.put(0x6b, Operations.copyValue(Register.E, Register.L));
        m.put(0x6c, Operations.copyValue(Register.H, Register.L));
        m.put(0x6d, Operations.copyValue(Register.L, Register.L));
        m.put(0x6e, Operations.copyValue(Register.HL, Register.L));
        m.put(0x70, Operations.copyValue(Register.B, Register.HL));
        m.put(0x71, Operations.copyValue(Register.C, Register.HL));
        m.put(0x72, Operations.copyValue(Register.D, Register.HL));
        m.put(0x73, Operations.copyValue(Register.E, Register.HL));
        m.put(0x74, Operations.copyValue(Register.H, Register.HL));
        m.put(0x75, Operations.copyValue(Register.L, Register.HL));
        m.put(0x36, Operations.loadValueTo(Register.HL));
        m.put(0x0a, Operations.copyValue(Register.BC, Register.A));
        m.put(0x1a, Operations.copyValue(Register.DE, Register.A));
        m.put(0xfa, Operations.loadValueIndirectTo(Register.A));
        m.put(0x3c, Operations.increment(Register.A));
        m.put(0x04, Operations.increment(Register.B));
        m.put(0x0c, Operations.increment(Register.C));
        m.put(0x14, Operations.increment(Register.D));
        m.put(0x1c, Operations.increment(Register.E));
        m.put(0x24, Operations.increment(Register.H));
        m.put(0x2c, Operations.increment(Register.L));
        m.put(0x34, Operations.increment(Register.HL));
        m.put(0xf2, Operations.copyFromIndirect(Register.C, Register.A));
        m.put(0xe2, Operations.copyToIndirect(Register.A, Register.C));
        m.put(0x3a, Operations.loadDecFromIndirect(Register.A, Register.HL));
        m.put(0x32, Operations.loadDecToIndirect(Register.HL, Register.A));
        m.put(0x2a, Operations.loadIncFromIndirect(Register.A, Register.HL));
        m.put(0x22, Operations.loadIncToIndirect(Register.HL, Register.A));
        m.put(0xe0, Operations.loadRegisterToAddress(Register.A));
        m.put(0xf0, Operations.loadAddressToRegister(Register.A));
        return m.build();
    }
}
