package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.IndirectAddress;

public class Operations {
    public static Operation nop() {
        return cpu -> 4;
    }

    public static Operation copyValue(Register from, Register to) {
        return cpu -> {
            cpu.set(to, cpu.readByte(from));
            return 4;
        };
    }

    public static Operation copyValue(IndirectAddress from, Register to) {
        return cpu -> {
            cpu.set(to, cpu.readByte(from));
            return 8;
        };
    }

    public static Operation copyValue(Register from, IndirectAddress to) {
        return cpu -> {
            cpu.setByte(to, cpu.readByte(from));
            return 8;
        };
    }

    public static Operation loadValueTo(Register r) {
        return cpu -> {
            int value = cpu.readNextArg();
            cpu.set(r, value);
            return 8;
        };
    }

    public static Operation loadValueTo(IndirectAddress address) {
        return cpu -> {
            int value = cpu.readNextArg();
            cpu.setByte(address, value);
            return 12;
        };
    }

    public static Operation loadValueIndirectTo(Register r) {
        return cpu -> {
            int lsb = cpu.readNextArg();
            int msb = cpu.readNextArg();
            int valueAtAddress = cpu.readByte((msb << 8) + lsb);
            cpu.set(Register.A, valueAtAddress);
            return 16;
        };
    }

    public static Operation increment(Register r) {
        return withZeroFlagHandler(r,
            withNibbleFlagHandler(r,
                withOpFlagSetTo(false,
                    cpu -> {
                        cpu.set(r, (cpu.readByte(r) + 1) & 0xff);
                        return 4;
                    }
                )
            )
        );
    }

    public static Operation increment(IndirectAddress address) {
        return withZeroFlagHandler(address,
            withNibbleFlagHandler(address,
                withOpFlagSetTo(false,
                    cpu -> {
                        cpu.setByte(cpu.resolveAddress(address), (cpu.readByte(address) + 1) & 0xff);
                        return 12;
                    }
                )
            )
        );
    }

    private static Operation withZeroFlagHandler(Register r, Operation inner) {
        return cpu -> {
            int ret = inner.apply(cpu);
            cpu.set(Flag.ZERO, (cpu.readByte(r) == 0x00));
            return ret;
        };
    }

    private static Operation withZeroFlagHandler(IndirectAddress address, Operation inner) {
        return cpu -> {
            int ret = inner.apply(cpu);
            cpu.set(Flag.ZERO, (cpu.readByte(address) == 0x00));
            return ret;
        };
    }

    private static Operation withNibbleFlagHandler(Register r, Operation inner) {
        return cpu -> {
            int before = cpu.readByte(r) & 0x10;
            int ret = inner.apply(cpu);
            int after = cpu.readByte(r) & 0x10;
            cpu.set(Flag.NIBBLE, (before < after));
            return ret;
        };
    }

    private static Operation withNibbleFlagHandler(IndirectAddress address, Operation inner) {
        return cpu -> {
            int before = cpu.readByte(address) & 0x10;
            int ret = inner.apply(cpu);
            int after = cpu.readByte(address) & 0x10;
            cpu.set(Flag.NIBBLE, (before < after));
            return ret;
        };
    }

    private static Operation withOpFlagSetTo(boolean flagValue, Operation inner) {
        return cpu -> {
            int ret = inner.apply(cpu);
            cpu.set(Flag.OPERATION, flagValue);
            return ret;
        };
    }

    public static Operation copyFromIndirect(Register fromLsb, Register to) {
        return cpu -> {
            int address = 0xff00 + cpu.readByte(fromLsb);
            cpu.set(to, cpu.readByte(address));
            return 8;
        };
    }

    public static Operation copyToIndirect(Register from, Register toLsb) {
        return cpu -> {
            int toSet = cpu.readByte(from);
            int targetAddr = 0xff00 + cpu.readByte(toLsb);
            cpu.setByte(targetAddr, toSet);
            return 8;
        };
    }

    public static Operation loadDecFromIndirect(Register to, IndirectAddress from) {
        return cpu -> {
            cpu.set(to, from);
            decrementPointer(from, cpu);
            return 8;
        };
    }

    public static Operation loadIncFromIndirect(Register to, IndirectAddress from) {
        return cpu -> {
            cpu.set(to, from);
            incrementPointer(from, cpu);
            return 8;
        };
    }

    public static Operation loadDecToIndirect(IndirectAddress to, Register from) {
        return cpu -> {
            cpu.setByte(to, from);
            decrementPointer(to, cpu);
            return 8;
        };
    }

    public static Operation loadIncToIndirect(IndirectAddress to, Register from) {
        return cpu -> {
            cpu.setByte(to, from);
            incrementPointer(to, cpu);
            return 8;
        };
    }

    private static void decrementPointer(IndirectAddress address, Cpu cpu) {
        int newLeft = cpu.readByte(address.left);
        if (cpu.readByte(address.right) == 0x00) {
            // Right-hand byte rolled back to 0xff,
            // so decrement left-hand byte and roll up to 0xff if needed.
            newLeft = (newLeft - 1) & 0xff;
        }

        // Decrement, and roll back up to 0xff if needed.
        int newRight = (cpu.readByte(address.right) - 1) & 0xff;

        cpu.set(address.left, newLeft);
        cpu.set(address.right, newRight);
    }

    private static void incrementPointer(IndirectAddress address, Cpu cpu) {
        int newLeft = cpu.readByte(address.left);
        if (cpu.readByte(address.right) == 0xff) {
            // Right-hand byte rolled over to 0x00,
            // so increment left-hand byte and roll over to 0xff if needed.
            newLeft = (newLeft + 1) & 0xff;
        }

        // Increment, and roll over to 0x00 if needed.
        int newRight = (cpu.readByte(address.right) + 1) & 0xff;

        cpu.set(address.left, newLeft);
        cpu.set(address.right, newRight);
    }
}
