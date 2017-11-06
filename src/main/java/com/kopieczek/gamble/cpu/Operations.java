package com.kopieczek.gamble.cpu;

class Operations {
    static int nop(Cpu cpu) {
        return 4;
    }

    static int copy(Cpu cpu, Byte.Register to, Byte.Register from) {
        cpu.set(to, from);
        return 4;
    }

    static int copy(Cpu cpu, Byte.Register to, Byte.Argument from) {
        cpu.set(to, from);
        return 8;
    }

    static int load(Cpu cpu, Byte.Register to, Pointer from) {
        cpu.set(to, from);
        return (from.address instanceof Word.Argument) ? 16 : 8;
    }

    static int write(Cpu cpu, Pointer to, Byte.Register from) {
        cpu.writeTo(to, from);
        return 8;
    }

    static int write(Cpu cpu, Pointer to, Byte.Argument from) {
        cpu.writeTo(to, from);
        return 12;
    }

    static int write(Cpu cpu, Pointer to, Word.Register from) {
        int fromValue = cpu.read(from);
        Byte fromLsb = Byte.literal(fromValue & 0xff);
        Byte fromMsb = Byte.literal(fromValue >> 8);
        cpu.writeTo(to, fromLsb);
        cpu.writeTo(to.withOffset(1), fromMsb);
        return 20;
    }

    static int increment(Cpu cpu, Byte.Register r) {
        int oldValue = cpu.read(r);
        int newValue = (oldValue + 1) & 0xff;
        cpu.set(r, Byte.literal(newValue));
        cpu.set(Flag.ZERO, (newValue == 0));
        cpu.set(Flag.NIBBLE, shouldSetNibble(oldValue, 1));
        cpu.set(Flag.OPERATION, false);
        return 4;
    }

    static int increment(Cpu cpu, Pointer ptr) {
        int oldValue = cpu.readFrom(ptr);
        int newValue = (oldValue + 1) & 0xff;
        cpu.writeTo(ptr, Byte.literal(newValue));
        cpu.set(Flag.ZERO, (newValue == 0));
        cpu.set(Flag.NIBBLE, shouldSetNibble(oldValue, 1));
        cpu.set(Flag.OPERATION, false);
        return 12;
    }

    static int loadPartial(Cpu cpu, Byte.Register to, Byte.Register fromLsb) {
        Pointer fromPtr = Pointer.literal(0xff00 + cpu.read(fromLsb));
        cpu.set(to, fromPtr);
        return 8;
    }

    static int loadPartial(Cpu cpu, Byte.Register to, Byte.Argument fromLsb) {
        Pointer fromPtr = Pointer.literal(0xff00 + cpu.read(fromLsb));
        cpu.set(to, fromPtr);
        return 12;
    }

    static int writePartial(Cpu cpu, Byte.Register toLsb, Byte.Register from) {
        Pointer toPtr = Pointer.literal(0xff00 + cpu.read(toLsb));
        cpu.writeTo(toPtr, from);
        return 8;
    }

    static int loadDec(Cpu cpu, Byte.Register to, Word.Register from) {
        cpu.set(to, Pointer.of(from));
        decrementWord(from, cpu);
        return 8;
    }

    static int loadInc(Cpu cpu, Byte.Register to, Word.Register from) {
        cpu.set(to, Pointer.of(from));
        incrementWord(from, cpu);
        return 8;
    }

    static int writeDec(Cpu cpu, Word.Register to, Byte from) {
        cpu.writeTo(Pointer.of(to), from);
        decrementWord(to, cpu);
        return 8;
    }

    static int writeInc(Cpu cpu, Word.Register to, Byte from) {
        cpu.writeTo(Pointer.of(to), from);
        incrementWord(to, cpu);
        return 8;
    }

    static int writePartial(Cpu cpu, Byte toLsb, Byte from) {
        Pointer to = Pointer.literal(0xff00 + cpu.read(toLsb));
        cpu.writeTo(to, from);
        return 12;
    }

    static int copy(Cpu cpu, Word.Register to, Word.Argument from) {
        cpu.set(to, from);
        return 12;
    }

    static int copy(Cpu cpu, Word.Register to, Word.Register from) {
        cpu.set(to, from);
        return 8;
    }

    static int copyWithOffset(Cpu cpu, Word.Register to, Word.Register from, Byte offset) {
        int fromValue = cpu.read(from);
        int offsetValue = cpu.read(offset);
        Word newValue = Word.literal((fromValue + offsetValue) & 0xffff);
        cpu.set(to, newValue);

        cpu.set(Flag.CARRY, shouldSetCarry(fromValue, offsetValue));
        cpu.set(Flag.NIBBLE, shouldSetNibble(fromValue, offsetValue));
        cpu.set(Flag.ZERO, false);
        cpu.set(Flag.OPERATION, false);

        return 12;
    }

    public static int push(Cpu cpu, Word.Register from) {
        Pointer stackPointer = Pointer.of(Word.Register.SP);
        decrementWord(Word.Register.SP, cpu);
        cpu.writeTo(stackPointer, from.left);
        decrementWord(Word.Register.SP, cpu);
        cpu.writeTo(stackPointer, from.right);
        return 16;
    }

    public static Integer pop(Cpu cpu, Word.Register to) {
        Pointer stackPointer = Pointer.of(Word.Register.SP);
        cpu.set(to.right, stackPointer);
        incrementWord(Word.Register.SP, cpu);
        cpu.set(to.left, stackPointer);
        incrementWord(Word.Register.SP, cpu);
        return 12;
    }

    private static void decrementWord(Word.Register r, Cpu cpu) {
        int current = cpu.read(r);
        Word next = Word.literal((current - 1) & 0xffff);
        cpu.set(r, next);
    }

    private static void incrementWord(Word.Register r, Cpu cpu) {
        int current = cpu.read(r);
        Word next = Word.literal((current + 1) & 0xffff);
        cpu.set(r, next);
    }

    private static boolean shouldSetCarry(int original, int offset) {
        return (((original & 0xff) + (offset & 0xff)) & 0x100) > 0;
    }

    private static boolean shouldSetNibble(int original, int offset) {
        return (((original & 0x0f) + (offset & 0x0f)) & 0x10) > 0;
    }
}
