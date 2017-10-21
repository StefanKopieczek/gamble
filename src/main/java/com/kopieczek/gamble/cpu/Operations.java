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
            cpu.pc += 1;
            cpu.set(r, cpu.readByte(cpu.pc));
            return 8;
        };
    }

    public static Operation loadValueTo(IndirectAddress address) {
        return cpu -> {
            cpu.pc += 1;
            cpu.setByte(address, cpu.readByte(cpu.pc));
            return 12;
        };
    }

    public static Operation loadValueIndirectTo(Register r) {
        return cpu -> {
            cpu.pc += 1;
            int lsb = cpu.readByte(cpu.pc);
            cpu.pc += 1;
            int msb = cpu.readByte(cpu.pc);
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
}
