package com.kopieczek.gamble.cpu;

public class Operations {
    public static Operation nop() {
        return cpu -> 4;
    }

    public static Operation copy(Byte.Register to, Byte.Register from) {
        return cpu -> {
            cpu.set(to, from);
            return 4;
        };
    }

    public static Operation copy(Byte.Register to, Byte.Argument from) {
        return cpu -> {
            cpu.set(to, from);
            return 8;
        };
    }

    public static Operation load(Byte.Register to, Pointer from) {
        return cpu -> {
            cpu.set(to, from);
            return 8;
        };
    }

    public static Operation load(Byte.Register to, Word.Argument fromAddr) {
        return cpu -> {
            Pointer from = Pointer.of(fromAddr);
            cpu.set(to, from);
            return 16;
        };
    }

    public static Operation write(Pointer to, Byte.Register from) {
        return cpu -> {
            cpu.writeTo(to, from);
            return 8;
        };
    }

    public static Operation write(Pointer to, Byte.Argument from) {
        return cpu -> {
            cpu.writeTo(to, from);
            return 12;
        };
    }

    public static Operation increment(Byte.Register r) {
        return cpu -> {
            int oldValue = cpu.read(r);
            int newValue = (oldValue + 1) & 0xff;
            cpu.set(r, Byte.literal(newValue));
            cpu.set(Flag.ZERO, (newValue == 0));
            cpu.set(Flag.NIBBLE, shouldSetNibble(oldValue, 1));
            cpu.set(Flag.OPERATION, false);
            return 4;
        };
    }

    public static Operation increment(Pointer ptr) {
        return cpu -> {
            int oldValue = cpu.readFrom(ptr);
            int newValue = (oldValue + 1) & 0xff;
            cpu.writeTo(ptr, Byte.literal(newValue));
            cpu.set(Flag.ZERO, (newValue == 0));
            cpu.set(Flag.NIBBLE, shouldSetNibble(oldValue, 1));
            cpu.set(Flag.OPERATION, false);
            return 12;
        };
    }

    public static Operation loadPartial(Byte.Register to, Byte fromLsb) {
        return cpu -> {
            Pointer fromPtr = Pointer.literal(0xff00 + cpu.read(fromLsb));
            cpu.set(to, fromPtr);
            return (fromLsb instanceof Byte.Argument) ? 12 : 8;
        };
    }

    public static Operation writePartial(Byte.Register toLsb, Byte from) {
        return cpu -> {
            Pointer toPtr = Pointer.literal(0xff00 + cpu.read(toLsb));
            cpu.writeTo(toPtr, from);
            return (from instanceof Byte.Argument) ? 12 : 8;
        };
    }

    public static Operation loadDec(Byte.Register to, Word.Register from) {
        return cpu -> {
            cpu.set(to, Pointer.of(from));
            decrementWord(from, cpu);
            return 8;
        };
    }

    public static Operation loadInc(Byte.Register to, Word.Register from) {
        return cpu -> {
            cpu.set(to, Pointer.of(from));
            incrementWord(from, cpu);
            return 8;
        };
    }

    public static Operation writeDec(Word.Register to, Byte from) {
        return cpu -> {
            cpu.writeTo(Pointer.of(to), from);
            decrementWord(to, cpu);
            return 8;
        };
    }

    public static Operation writeInc(Word.Register to, Byte from) {
        return cpu -> {
            cpu.writeTo(Pointer.of(to), from);
            incrementWord(to, cpu);
            return 8;
        };
    }

    public static Operation writePartial(Byte toLsb, Byte from) {
        return cpu -> {
            Pointer to = Pointer.literal(0xff00 + cpu.read(toLsb));
            cpu.writeTo(to, from);
            return 12;
        };
    }

    public static Operation copy(Word.Register to, Word.Argument from) {
        return cpu -> {
            cpu.set(to, from);
            return 12;
        };
    }

    public static Operation copy(Word.Register to, Word.Register from) {
        return cpu -> {
            cpu.set(to, from);
            return 8;
        };
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

    public static Operation copyWithOffset(Word.Register to, Word.Register from, Byte offset) {
        return cpu -> {
            int fromValue = cpu.read(from);
            int offsetValue = cpu.read(offset);
            Word newValue = Word.literal((fromValue + offsetValue) & 0xffff);
            cpu.set(to, newValue);

            cpu.set(Flag.CARRY, shouldSetCarry(fromValue, offsetValue));
            cpu.set(Flag.NIBBLE, shouldSetNibble(fromValue, offsetValue));
            cpu.set(Flag.ZERO, false);
            cpu.set(Flag.OPERATION, false);

            return 12;
        };
    }
}
