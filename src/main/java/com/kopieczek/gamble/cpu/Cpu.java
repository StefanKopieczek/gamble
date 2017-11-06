package com.kopieczek.gamble.cpu;

import com.google.common.collect.ImmutableMap;
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
        this.registers = new int[Byte.Register.values().length];
        this.flags = new boolean[Flag.values().length];
    }

    public int read(Byte b) {
        return b.getValue(this);
    }

    public int read(Word w) {
        return w.getValue(this);
    }

    public int readFrom(Pointer ptr) {
        return ptr.get(this);
    }

    public void set(Byte.Register to, Byte from) {
        registers[to.ordinal()] = from.getValue(this);
    }

    public void set(Byte.Register to, Pointer from) {
        set(to, Byte.literal(readFrom(from)));
    }

    public void set(Word.Register to, Word from) {
        int fromValue = read(from);
        set(to.left, Byte.literal(fromValue >> 8));
        set(to.right, Byte.literal(fromValue & 0xff));
    }

    public void writeTo(Pointer ptr, Byte from) {
        ptr.set(read(from), this);
    }

    int unsafeRead(int address) {
        return mmu.readByte(address);
    }

    void unsafeSet(int address, int value) {
        mmu.setByte(address, value);
    }

    int readNextArg() {
        pc += 1;
        return unsafeRead(pc);
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
        m.put(0x3e, Operations.copy(Byte.Register.A, Byte.argument()));
        m.put(0x06, Operations.copy(Byte.Register.B, Byte.argument()));
        m.put(0x0e, Operations.copy(Byte.Register.C, Byte.argument()));
        m.put(0x16, Operations.copy(Byte.Register.D, Byte.argument()));
        m.put(0x1e, Operations.copy(Byte.Register.E, Byte.argument()));
        m.put(0x26, Operations.copy(Byte.Register.H, Byte.argument()));
        m.put(0x2e, Operations.copy(Byte.Register.L, Byte.argument()));
        m.put(0x7f, Operations.nop());
        m.put(0x78, Operations.copy(Byte.Register.A, Byte.Register.B));
        m.put(0x79, Operations.copy(Byte.Register.A, Byte.Register.C));
        m.put(0x7a, Operations.copy(Byte.Register.A, Byte.Register.D));
        m.put(0x7b, Operations.copy(Byte.Register.A, Byte.Register.E));
        m.put(0x7c, Operations.copy(Byte.Register.A, Byte.Register.H));
        m.put(0x7d, Operations.copy(Byte.Register.A, Byte.Register.L));
        m.put(0x7e, Operations.load(Byte.Register.A, Pointer.of(Word.Register.HL)));
        m.put(0x40, Operations.copy(Byte.Register.B, Byte.Register.B));
        m.put(0x41, Operations.copy(Byte.Register.B, Byte.Register.C));
        m.put(0x42, Operations.copy(Byte.Register.B, Byte.Register.D));
        m.put(0x43, Operations.copy(Byte.Register.B, Byte.Register.E));
        m.put(0x44, Operations.copy(Byte.Register.B, Byte.Register.H));
        m.put(0x45, Operations.copy(Byte.Register.B, Byte.Register.L));
        m.put(0x46, Operations.load(Byte.Register.B, Pointer.of(Word.Register.HL)));
        m.put(0x48, Operations.copy(Byte.Register.C, Byte.Register.B));
        m.put(0x49, Operations.copy(Byte.Register.C, Byte.Register.C));
        m.put(0x4a, Operations.copy(Byte.Register.C, Byte.Register.D));
        m.put(0x4b, Operations.copy(Byte.Register.C, Byte.Register.E));
        m.put(0x4c, Operations.copy(Byte.Register.C, Byte.Register.H));
        m.put(0x4d, Operations.copy(Byte.Register.C, Byte.Register.L));
        m.put(0x4e, Operations.load(Byte.Register.C, Pointer.of(Word.Register.HL)));
        m.put(0x50, Operations.copy(Byte.Register.D, Byte.Register.B));
        m.put(0x51, Operations.copy(Byte.Register.D, Byte.Register.C));
        m.put(0x52, Operations.copy(Byte.Register.D, Byte.Register.D));
        m.put(0x53, Operations.copy(Byte.Register.D, Byte.Register.E));
        m.put(0x54, Operations.copy(Byte.Register.D, Byte.Register.H));
        m.put(0x55, Operations.copy(Byte.Register.D, Byte.Register.L));
        m.put(0x56, Operations.load(Byte.Register.D, Pointer.of(Word.Register.HL)));
        m.put(0x58, Operations.copy(Byte.Register.E, Byte.Register.B));
        m.put(0x59, Operations.copy(Byte.Register.E, Byte.Register.C));
        m.put(0x5a, Operations.copy(Byte.Register.E, Byte.Register.D));
        m.put(0x5b, Operations.copy(Byte.Register.E, Byte.Register.E));
        m.put(0x5c, Operations.copy(Byte.Register.E, Byte.Register.H));
        m.put(0x5d, Operations.copy(Byte.Register.E, Byte.Register.L));
        m.put(0x5e, Operations.load(Byte.Register.E, Pointer.of(Word.Register.HL)));
        m.put(0x60, Operations.copy(Byte.Register.H, Byte.Register.B));
        m.put(0x61, Operations.copy(Byte.Register.H, Byte.Register.C));
        m.put(0x62, Operations.copy(Byte.Register.H, Byte.Register.D));
        m.put(0x63, Operations.copy(Byte.Register.H, Byte.Register.E));
        m.put(0x64, Operations.copy(Byte.Register.H, Byte.Register.H));
        m.put(0x65, Operations.copy(Byte.Register.H, Byte.Register.L));
        m.put(0x66, Operations.load(Byte.Register.H, Pointer.of(Word.Register.HL)));
        m.put(0x68, Operations.copy(Byte.Register.L, Byte.Register.B));
        m.put(0x69, Operations.copy(Byte.Register.L, Byte.Register.C));
        m.put(0x6a, Operations.copy(Byte.Register.L, Byte.Register.D));
        m.put(0x6b, Operations.copy(Byte.Register.L, Byte.Register.E));
        m.put(0x6c, Operations.copy(Byte.Register.L, Byte.Register.H));
        m.put(0x6d, Operations.copy(Byte.Register.L, Byte.Register.L));
        m.put(0x6e, Operations.load(Byte.Register.L, Pointer.of(Word.Register.HL)));
        m.put(0x70, Operations.write(Pointer.of(Word.Register.HL), Byte.Register.B));
        m.put(0x71, Operations.write(Pointer.of(Word.Register.HL), Byte.Register.C));
        m.put(0x72, Operations.write(Pointer.of(Word.Register.HL), Byte.Register.D));
        m.put(0x73, Operations.write(Pointer.of(Word.Register.HL), Byte.Register.E));
        m.put(0x74, Operations.write(Pointer.of(Word.Register.HL), Byte.Register.H));
        m.put(0x75, Operations.write(Pointer.of(Word.Register.HL), Byte.Register.L));
        m.put(0x36, Operations.write(Pointer.of(Word.Register.HL), Byte.argument()));
        m.put(0x0a, Operations.load(Byte.Register.A, Pointer.of(Word.Register.BC)));
        m.put(0x1a, Operations.load(Byte.Register.A, Pointer.of(Word.Register.DE)));
        m.put(0xfa, Operations.load(Byte.Register.A, Pointer.of(Word.argument())));
        m.put(0x3c, Operations.increment(Byte.Register.A));
        m.put(0x04, Operations.increment(Byte.Register.B));
        m.put(0x0c, Operations.increment(Byte.Register.C));
        m.put(0x14, Operations.increment(Byte.Register.D));
        m.put(0x1c, Operations.increment(Byte.Register.E));
        m.put(0x24, Operations.increment(Byte.Register.H));
        m.put(0x2c, Operations.increment(Byte.Register.L));
        m.put(0x34, Operations.increment(Pointer.of(Word.Register.HL)));
        m.put(0xf2, Operations.loadPartial(Byte.Register.A, Byte.Register.C));
        m.put(0xe2, Operations.writePartial(Byte.Register.C, Byte.Register.A));
        m.put(0x3a, Operations.loadDec(Byte.Register.A, Word.Register.HL));
        m.put(0x32, Operations.writeDec(Word.Register.HL, Byte.Register.A));
        m.put(0x2a, Operations.loadInc(Byte.Register.A, Word.Register.HL));
        m.put(0x22, Operations.writeInc(Word.Register.HL, Byte.Register.A));
        m.put(0xe0, Operations.writePartial(Byte.argument(), Byte.Register.A));
        m.put(0xf0, Operations.loadPartial(Byte.Register.A, Byte.argument()));
        m.put(0x01, Operations.copy(Word.Register.BC, Word.argument()));
        m.put(0x11, Operations.copy(Word.Register.DE, Word.argument()));
        m.put(0x21, Operations.copy(Word.Register.HL, Word.argument()));
        m.put(0x31, Operations.copy(Word.Register.SP, Word.argument()));
        m.put(0xf9, Operations.copy(Word.Register.SP, Word.Register.HL));
        m.put(0xf8, Operations.copyWithOffset(Word.Register.HL, Word.Register.SP, Byte.argument()));
        return m.build();
    }
}
