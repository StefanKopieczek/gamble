package com.kopieczek.gamble.cpu;

import com.google.common.collect.ImmutableMap;
import com.kopieczek.gamble.memory.MemoryManagementUnit;

import java.util.Map;
import java.util.function.Function;

public class Cpu {
    final MemoryManagementUnit mmu;
    static final Map<Integer, Function<Cpu, Integer>> operations = loadOperations();
    int pc = 0;
    int cycles = 0;
    int[] registers;

    public Cpu(MemoryManagementUnit mmu) {
        this.mmu = mmu;
        this.registers = new int[Byte.Register.values().length];
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

        Function<Cpu, Integer> op = operations.get(opcode);
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
        return (read(Byte.Register.F) & (1 << (flag.ordinal() + 4))) > 0;
    }

    void set(Flag flag, boolean shouldEnable) {
        int flagRegister = read(Byte.Register.F);
        int flagMask = (1 << (flag.ordinal() + 4));
        if (shouldEnable) {
            flagRegister |= flagMask;
        } else {
            flagRegister &= ~flagMask;
        }
        set(Byte.Register.F, Byte.literal(flagRegister));
    }

    private static Map<Integer, Function<Cpu, Integer>> loadOperations() {
        ImmutableMap.Builder<Integer, Function<Cpu, Integer>> m = ImmutableMap.builder();
        m.put(0x00, cpu -> Operations.nop(cpu));
        m.put(0x3e, cpu -> Operations.copy(cpu, Byte.Register.A, Byte.argument()));
        m.put(0x06, cpu -> Operations.copy(cpu, Byte.Register.B, Byte.argument()));
        m.put(0x0e, cpu -> Operations.copy(cpu, Byte.Register.C, Byte.argument()));
        m.put(0x16, cpu -> Operations.copy(cpu, Byte.Register.D, Byte.argument()));
        m.put(0x1e, cpu -> Operations.copy(cpu, Byte.Register.E, Byte.argument()));
        m.put(0x26, cpu -> Operations.copy(cpu, Byte.Register.H, Byte.argument()));
        m.put(0x2e, cpu -> Operations.copy(cpu, Byte.Register.L, Byte.argument()));
        m.put(0x7f, cpu -> Operations.nop(cpu));
        m.put(0x78, cpu -> Operations.copy(cpu, Byte.Register.A, Byte.Register.B));
        m.put(0x79, cpu -> Operations.copy(cpu, Byte.Register.A, Byte.Register.C));
        m.put(0x7a, cpu -> Operations.copy(cpu, Byte.Register.A, Byte.Register.D));
        m.put(0x7b, cpu -> Operations.copy(cpu, Byte.Register.A, Byte.Register.E));
        m.put(0x7c, cpu -> Operations.copy(cpu, Byte.Register.A, Byte.Register.H));
        m.put(0x7d, cpu -> Operations.copy(cpu, Byte.Register.A, Byte.Register.L));
        m.put(0x7e, cpu -> Operations.load(cpu, Byte.Register.A, Pointer.of(Word.Register.HL)));
        m.put(0x40, cpu -> Operations.copy(cpu, Byte.Register.B, Byte.Register.B));
        m.put(0x41, cpu -> Operations.copy(cpu, Byte.Register.B, Byte.Register.C));
        m.put(0x42, cpu -> Operations.copy(cpu, Byte.Register.B, Byte.Register.D));
        m.put(0x43, cpu -> Operations.copy(cpu, Byte.Register.B, Byte.Register.E));
        m.put(0x44, cpu -> Operations.copy(cpu, Byte.Register.B, Byte.Register.H));
        m.put(0x45, cpu -> Operations.copy(cpu, Byte.Register.B, Byte.Register.L));
        m.put(0x46, cpu -> Operations.load(cpu, Byte.Register.B, Pointer.of(Word.Register.HL)));
        m.put(0x48, cpu -> Operations.copy(cpu, Byte.Register.C, Byte.Register.B));
        m.put(0x49, cpu -> Operations.copy(cpu, Byte.Register.C, Byte.Register.C));
        m.put(0x4a, cpu -> Operations.copy(cpu, Byte.Register.C, Byte.Register.D));
        m.put(0x4b, cpu -> Operations.copy(cpu, Byte.Register.C, Byte.Register.E));
        m.put(0x4c, cpu -> Operations.copy(cpu, Byte.Register.C, Byte.Register.H));
        m.put(0x4d, cpu -> Operations.copy(cpu, Byte.Register.C, Byte.Register.L));
        m.put(0x4e, cpu -> Operations.load(cpu, Byte.Register.C, Pointer.of(Word.Register.HL)));
        m.put(0x50, cpu -> Operations.copy(cpu, Byte.Register.D, Byte.Register.B));
        m.put(0x51, cpu -> Operations.copy(cpu, Byte.Register.D, Byte.Register.C));
        m.put(0x52, cpu -> Operations.copy(cpu, Byte.Register.D, Byte.Register.D));
        m.put(0x53, cpu -> Operations.copy(cpu, Byte.Register.D, Byte.Register.E));
        m.put(0x54, cpu -> Operations.copy(cpu, Byte.Register.D, Byte.Register.H));
        m.put(0x55, cpu -> Operations.copy(cpu, Byte.Register.D, Byte.Register.L));
        m.put(0x56, cpu -> Operations.load(cpu, Byte.Register.D, Pointer.of(Word.Register.HL)));
        m.put(0x58, cpu -> Operations.copy(cpu, Byte.Register.E, Byte.Register.B));
        m.put(0x59, cpu -> Operations.copy(cpu, Byte.Register.E, Byte.Register.C));
        m.put(0x5a, cpu -> Operations.copy(cpu, Byte.Register.E, Byte.Register.D));
        m.put(0x5b, cpu -> Operations.copy(cpu, Byte.Register.E, Byte.Register.E));
        m.put(0x5c, cpu -> Operations.copy(cpu, Byte.Register.E, Byte.Register.H));
        m.put(0x5d, cpu -> Operations.copy(cpu, Byte.Register.E, Byte.Register.L));
        m.put(0x5e, cpu -> Operations.load(cpu, Byte.Register.E, Pointer.of(Word.Register.HL)));
        m.put(0x60, cpu -> Operations.copy(cpu, Byte.Register.H, Byte.Register.B));
        m.put(0x61, cpu -> Operations.copy(cpu, Byte.Register.H, Byte.Register.C));
        m.put(0x62, cpu -> Operations.copy(cpu, Byte.Register.H, Byte.Register.D));
        m.put(0x63, cpu -> Operations.copy(cpu, Byte.Register.H, Byte.Register.E));
        m.put(0x64, cpu -> Operations.copy(cpu, Byte.Register.H, Byte.Register.H));
        m.put(0x65, cpu -> Operations.copy(cpu, Byte.Register.H, Byte.Register.L));
        m.put(0x66, cpu -> Operations.load(cpu, Byte.Register.H, Pointer.of(Word.Register.HL)));
        m.put(0x68, cpu -> Operations.copy(cpu, Byte.Register.L, Byte.Register.B));
        m.put(0x69, cpu -> Operations.copy(cpu, Byte.Register.L, Byte.Register.C));
        m.put(0x6a, cpu -> Operations.copy(cpu, Byte.Register.L, Byte.Register.D));
        m.put(0x6b, cpu -> Operations.copy(cpu, Byte.Register.L, Byte.Register.E));
        m.put(0x6c, cpu -> Operations.copy(cpu, Byte.Register.L, Byte.Register.H));
        m.put(0x6d, cpu -> Operations.copy(cpu, Byte.Register.L, Byte.Register.L));
        m.put(0x6e, cpu -> Operations.load(cpu, Byte.Register.L, Pointer.of(Word.Register.HL)));
        m.put(0x70, cpu -> Operations.write(cpu, Pointer.of(Word.Register.HL), Byte.Register.B));
        m.put(0x71, cpu -> Operations.write(cpu, Pointer.of(Word.Register.HL), Byte.Register.C));
        m.put(0x72, cpu -> Operations.write(cpu, Pointer.of(Word.Register.HL), Byte.Register.D));
        m.put(0x73, cpu -> Operations.write(cpu, Pointer.of(Word.Register.HL), Byte.Register.E));
        m.put(0x74, cpu -> Operations.write(cpu, Pointer.of(Word.Register.HL), Byte.Register.H));
        m.put(0x75, cpu -> Operations.write(cpu, Pointer.of(Word.Register.HL), Byte.Register.L));
        m.put(0x36, cpu -> Operations.write(cpu, Pointer.of(Word.Register.HL), Byte.argument()));
        m.put(0x0a, cpu -> Operations.load(cpu, Byte.Register.A, Pointer.of(Word.Register.BC)));
        m.put(0x1a, cpu -> Operations.load(cpu, Byte.Register.A, Pointer.of(Word.Register.DE)));
        m.put(0xfa, cpu -> Operations.load(cpu, Byte.Register.A, Pointer.of(Word.argument())));
        m.put(0x3c, cpu -> Operations.increment(cpu, Byte.Register.A));
        m.put(0x04, cpu -> Operations.increment(cpu, Byte.Register.B));
        m.put(0x0c, cpu -> Operations.increment(cpu, Byte.Register.C));
        m.put(0x14, cpu -> Operations.increment(cpu, Byte.Register.D));
        m.put(0x1c, cpu -> Operations.increment(cpu, Byte.Register.E));
        m.put(0x24, cpu -> Operations.increment(cpu, Byte.Register.H));
        m.put(0x2c, cpu -> Operations.increment(cpu, Byte.Register.L));
        m.put(0x34, cpu -> Operations.increment(cpu, Pointer.of(Word.Register.HL)));
        m.put(0xf2, cpu -> Operations.loadPartial(cpu, Byte.Register.A, Byte.Register.C));
        m.put(0xe2, cpu -> Operations.writePartial(cpu, Byte.Register.C, Byte.Register.A));
        m.put(0x3a, cpu -> Operations.loadDec(cpu, Byte.Register.A, Word.Register.HL));
        m.put(0x32, cpu -> Operations.writeDec(cpu, Word.Register.HL, Byte.Register.A));
        m.put(0x2a, cpu -> Operations.loadInc(cpu, Byte.Register.A, Word.Register.HL));
        m.put(0x22, cpu -> Operations.writeInc(cpu, Word.Register.HL, Byte.Register.A));
        m.put(0xe0, cpu -> Operations.writePartial(cpu, Byte.argument(), Byte.Register.A));
        m.put(0xf0, cpu -> Operations.loadPartial(cpu, Byte.Register.A, Byte.argument()));
        m.put(0x01, cpu -> Operations.copy(cpu, Word.Register.BC, Word.argument()));
        m.put(0x11, cpu -> Operations.copy(cpu, Word.Register.DE, Word.argument()));
        m.put(0x21, cpu -> Operations.copy(cpu, Word.Register.HL, Word.argument()));
        m.put(0x31, cpu -> Operations.copy(cpu, Word.Register.SP, Word.argument()));
        m.put(0xf9, cpu -> Operations.copy(cpu, Word.Register.SP, Word.Register.HL));
        m.put(0xf8, cpu -> Operations.copyWithOffset(cpu, Word.Register.HL, Word.Register.SP, Byte.argument()));
        m.put(0x08, cpu -> Operations.write(cpu, Pointer.of(Word.argument()), Word.Register.SP));
        m.put(0xf5, cpu -> Operations.push(cpu, Word.Register.AF));
        m.put(0xc5, cpu -> Operations.push(cpu, Word.Register.BC));
        m.put(0xd5, cpu -> Operations.push(cpu, Word.Register.DE));
        m.put(0xe5, cpu -> Operations.push(cpu, Word.Register.HL));
        m.put(0xf1, cpu -> Operations.pop(cpu, Word.Register.AF));
        m.put(0xc1, cpu -> Operations.pop(cpu, Word.Register.BC));
        m.put(0xd1, cpu -> Operations.pop(cpu, Word.Register.DE));
        m.put(0xe1, cpu -> Operations.pop(cpu, Word.Register.HL));
        m.put(0x87, cpu -> Operations.add(cpu, Byte.Register.A, Byte.Register.A));
        m.put(0x80, cpu -> Operations.add(cpu, Byte.Register.A, Byte.Register.B));
        m.put(0x81, cpu -> Operations.add(cpu, Byte.Register.A, Byte.Register.C));
        m.put(0x82, cpu -> Operations.add(cpu, Byte.Register.A, Byte.Register.D));
        m.put(0x83, cpu -> Operations.add(cpu, Byte.Register.A, Byte.Register.E));
        m.put(0x84, cpu -> Operations.add(cpu, Byte.Register.A, Byte.Register.H));
        m.put(0x85, cpu -> Operations.add(cpu, Byte.Register.A, Byte.Register.L));
        m.put(0x86, cpu -> Operations.add(cpu, Byte.Register.A, Pointer.of(Word.Register.HL)));
        m.put(0xc6, cpu -> Operations.add(cpu, Byte.Register.A, Byte.argument()));
        m.put(0x8f, cpu -> Operations.addWithCarry(cpu, Byte.Register.A, Byte.Register.A));
        m.put(0x88, cpu -> Operations.addWithCarry(cpu, Byte.Register.A, Byte.Register.B));
        m.put(0x89, cpu -> Operations.addWithCarry(cpu, Byte.Register.A, Byte.Register.C));
        m.put(0x8a, cpu -> Operations.addWithCarry(cpu, Byte.Register.A, Byte.Register.D));
        m.put(0x8b, cpu -> Operations.addWithCarry(cpu, Byte.Register.A, Byte.Register.E));
        m.put(0x8c, cpu -> Operations.addWithCarry(cpu, Byte.Register.A, Byte.Register.H));
        m.put(0x8d, cpu -> Operations.addWithCarry(cpu, Byte.Register.A, Byte.Register.L));
        m.put(0x8e, cpu -> Operations.addWithCarry(cpu, Byte.Register.A, Pointer.of(Word.Register.HL)));
        m.put(0xce, cpu -> Operations.addWithCarry(cpu, Byte.Register.A, Byte.argument()));
        m.put(0x97, cpu -> Operations.subtract(cpu, Byte.Register.A, Byte.Register.A));
        m.put(0x90, cpu -> Operations.subtract(cpu, Byte.Register.A, Byte.Register.B));
        m.put(0x91, cpu -> Operations.subtract(cpu, Byte.Register.A, Byte.Register.C));
        m.put(0x92, cpu -> Operations.subtract(cpu, Byte.Register.A, Byte.Register.D));
        m.put(0x93, cpu -> Operations.subtract(cpu, Byte.Register.A, Byte.Register.E));
        m.put(0x94, cpu -> Operations.subtract(cpu, Byte.Register.A, Byte.Register.H));
        m.put(0x95, cpu -> Operations.subtract(cpu, Byte.Register.A, Byte.Register.L));
        m.put(0x96, cpu -> Operations.subtract(cpu, Byte.Register.A, Pointer.of(Word.Register.HL)));
        m.put(0xd6, cpu -> Operations.subtract(cpu, Byte.Register.A, Byte.argument()));
        m.put(0x9f, cpu -> Operations.subtractWithCarry(cpu, Byte.Register.A, Byte.Register.A));
        m.put(0x98, cpu -> Operations.subtractWithCarry(cpu, Byte.Register.A, Byte.Register.B));
        m.put(0x99, cpu -> Operations.subtractWithCarry(cpu, Byte.Register.A, Byte.Register.C));
        m.put(0x9a, cpu -> Operations.subtractWithCarry(cpu, Byte.Register.A, Byte.Register.D));
        m.put(0x9b, cpu -> Operations.subtractWithCarry(cpu, Byte.Register.A, Byte.Register.E));
        m.put(0x9c, cpu -> Operations.subtractWithCarry(cpu, Byte.Register.A, Byte.Register.H));
        m.put(0x9d, cpu -> Operations.subtractWithCarry(cpu, Byte.Register.A, Byte.Register.L));
        m.put(0x9e, cpu -> Operations.subtractWithCarry(cpu, Byte.Register.A, Pointer.of(Word.Register.HL)));
        m.put(0xde, cpu -> Operations.subtractWithCarry(cpu, Byte.Register.A, Byte.argument()));
        m.put(0xa7, cpu -> Operations.and(cpu, Byte.Register.A, Byte.Register.A));
        m.put(0xa0, cpu -> Operations.and(cpu, Byte.Register.A, Byte.Register.B));
        m.put(0xa1, cpu -> Operations.and(cpu, Byte.Register.A, Byte.Register.C));
        m.put(0xa2, cpu -> Operations.and(cpu, Byte.Register.A, Byte.Register.D));
        m.put(0xa3, cpu -> Operations.and(cpu, Byte.Register.A, Byte.Register.E));
        m.put(0xa4, cpu -> Operations.and(cpu, Byte.Register.A, Byte.Register.H));
        m.put(0xa5, cpu -> Operations.and(cpu, Byte.Register.A, Byte.Register.L));
        m.put(0xa6, cpu -> Operations.and(cpu, Byte.Register.A, Pointer.of(Word.Register.HL)));
        m.put(0xe6, cpu -> Operations.and(cpu, Byte.Register.A, Byte.argument()));
        m.put(0xb7, cpu -> Operations.or(cpu, Byte.Register.A, Byte.Register.A));
        m.put(0xb0, cpu -> Operations.or(cpu, Byte.Register.A, Byte.Register.B));
        m.put(0xb1, cpu -> Operations.or(cpu, Byte.Register.A, Byte.Register.C));
        m.put(0xb2, cpu -> Operations.or(cpu, Byte.Register.A, Byte.Register.D));
        m.put(0xb3, cpu -> Operations.or(cpu, Byte.Register.A, Byte.Register.E));
        m.put(0xb4, cpu -> Operations.or(cpu, Byte.Register.A, Byte.Register.H));
        m.put(0xb5, cpu -> Operations.or(cpu, Byte.Register.A, Byte.Register.L));
        m.put(0xb6, cpu -> Operations.or(cpu, Byte.Register.A, Pointer.of(Word.Register.HL)));
        m.put(0xf6, cpu -> Operations.or(cpu, Byte.Register.A, Byte.argument()));
        m.put(0xaf, cpu -> Operations.xor(cpu, Byte.Register.A, Byte.Register.A));
        m.put(0xa8, cpu -> Operations.xor(cpu, Byte.Register.A, Byte.Register.B));
        m.put(0xa9, cpu -> Operations.xor(cpu, Byte.Register.A, Byte.Register.C));
        m.put(0xaa, cpu -> Operations.xor(cpu, Byte.Register.A, Byte.Register.D));
        m.put(0xab, cpu -> Operations.xor(cpu, Byte.Register.A, Byte.Register.E));
        m.put(0xac, cpu -> Operations.xor(cpu, Byte.Register.A, Byte.Register.H));
        m.put(0xad, cpu -> Operations.xor(cpu, Byte.Register.A, Byte.Register.L));
        m.put(0xae, cpu -> Operations.xor(cpu, Byte.Register.A, Pointer.of(Word.Register.HL)));
        m.put(0xee, cpu -> Operations.xor(cpu, Byte.Register.A, Byte.argument()));
        m.put(0xb8, cpu -> Operations.compare(cpu, Byte.Register.A, Byte.Register.B));
        return m.build();
    }
}
