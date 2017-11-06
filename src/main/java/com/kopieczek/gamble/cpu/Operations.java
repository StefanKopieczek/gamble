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

    public static Operation load(Byte.Register to, Byte.Argument arg1, Byte.Argument arg2) {
        return cpu -> {
            Pointer from = Pointer.literal(arg1.getValue(cpu) + (arg2.getValue(cpu) << 8));
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
        return withZeroFlagHandler(r,
            withNibbleFlagHandler(r,
                withReset(Flag.OPERATION, cpu -> {
                        Byte newValue = Byte.literal((cpu.read(r) + 1) & 0xff);
                        cpu.set(r, newValue);
                        return 4;
                    }
                )
            )
        );
    }

    public static Operation increment(Pointer ptr) {
        return withZeroFlagHandler(ptr,
            withNibbleFlagHandler(ptr,
                withReset(Flag.OPERATION, cpu -> {
                        Byte newValue = Byte.literal((cpu.readFrom(ptr) + 1) & 0xff);
                        cpu.writeTo(ptr, newValue);
                        return 12;
                    }
                )
            )
        );
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

    private static Operation withZeroFlagHandler(Byte.Register r, Operation inner) {
        return cpu -> {
            int ret = inner.apply(cpu);
            cpu.set(Flag.ZERO, (cpu.read(r) == 0x00));
            return ret;
        };
    }

    private static Operation withZeroFlagHandler(Pointer ptr, Operation inner) {
        return cpu -> {
            int ret = inner.apply(cpu);
            cpu.set(Flag.ZERO, (cpu.readFrom(ptr) == 0x00));
            return ret;
        };
    }

    private static Operation withCarryFlagHandler(Word.Register before,
                                                  Word.Register after,
                                                  Operation inner) {
        return cpu -> {
            int beforeVal = cpu.read(before);
            int cycles = inner.apply(cpu);
            int afterVal = cpu.read(after);
            boolean carried = (afterVal & 0x0100) != (beforeVal & 0x0100);
            cpu.set(Flag.CARRY, carried);
            return cycles;
        };
    }

    private static Operation withNibbleFlagHandler(Byte.Register r, Operation inner) {
        return cpu -> {
            int before = cpu.read(r) & 0x10;
            int ret = inner.apply(cpu);
            int after = cpu.read(r) & 0x10;
            cpu.set(Flag.NIBBLE, (before < after));
            return ret;
        };
    }

    private static Operation withNibbleFlagHandler(Pointer ptr, Operation inner) {
        return cpu -> {
            int before = cpu.readFrom(ptr) & 0x10;
            int ret = inner.apply(cpu);
            int after = cpu.readFrom(ptr) & 0x10;
            cpu.set(Flag.NIBBLE, (before < after));
            return ret;
        };
    }

    private static Operation withReset(Flag flag, Operation inner) {
        return cpu -> {
            int ret = inner.apply(cpu);
            cpu.set(flag, false);
            return ret;
        };
    }

    public static Operation copyWithOffset(Word.Register to, Word.Register from, Byte offset) {
        return withCarryFlagHandler(from, to,
                withReset(Flag.ZERO,
                withReset(Flag.OPERATION,
                cpu -> {
                    int fromValue = cpu.read(from);
                    int offsetValue = cpu.read(offset);
                    Word newValue = Word.literal((fromValue + offsetValue) & 0xffff);
                    cpu.set(to, newValue);

                    boolean halfCarry = (((fromValue & 0x0f) + (offsetValue & 0x0f)) & 0x10) > 0;
                    cpu.set(Flag.NIBBLE, halfCarry);

                    return 12;
                }
        )));
    }
}
