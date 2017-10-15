package com.kopieczek.gamble.cpu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

enum Operation {
    NOP(0x00, null, FlagStrategy.NONE, 4, (cpu) -> null),
    INC_A(0x3c, Register.A, FlagStrategy.RESET, 4, (cpu) -> {
        cpu.increment(Register.A);
        return null;
    }),
    INC_B(0x04, Register.B, FlagStrategy.RESET, 4, (cpu) -> {
        cpu.increment(Register.B);
        return null;
    }),
    INC_C(0x0c, Register.C, FlagStrategy.RESET, 4, (cpu) -> {
        cpu.increment(Register.C);
        return null;
    }),
    INC_D(0x14, Register.D, FlagStrategy.RESET, 4, (cpu) -> {
        cpu.increment(Register.D);
        return null;
    }),
    INC_E(0x1c, Register.E, FlagStrategy.RESET, 4, (cpu) -> {
        cpu.increment(Register.E);
        return null;
    }),
    INC_H(0x24, Register.H, FlagStrategy.RESET, 4, (cpu) -> {
        cpu.increment(Register.H);
        return null;
    }),
    INC_L(0x2c, Register.L, FlagStrategy.RESET, 4, (cpu) -> {
        cpu.increment(Register.L);
        return null;
    }),
    INC_HL(0x34, Register.H, FlagStrategy.RESET, 12, (cpu) -> {
        cpu.increment(Register.L);
        if (cpu.readByte(Register.L) == 0x00) {
            cpu.increment(Register.H);
        }
        return null;
    });

    static final Map<Integer, Operation> map = new HashMap<>();
    static {
        for (Operation op : Operation.values()) {
            map.put(op.opcode, op);
        }
    }

    private int opcode;
    private Register maybeZeroRegister;
    private FlagStrategy opFlagStrategy;
    int cycleCount;
    Function<Cpu, Void> action;

    Operation(int opcode, Register maybeZeroRegister, FlagStrategy opFlagStrategy, int cycleCount,
              Function<Cpu, Void> action) {
        this.opcode = opcode;
        this.maybeZeroRegister = maybeZeroRegister;
        this.opFlagStrategy = opFlagStrategy;
        this.cycleCount = cycleCount;
        this.action = action;
    }

    public void execute(Cpu cpu) {
        cpu.cycles += cycleCount;
        action.apply(cpu);
        if (maybeZeroRegister != null) {
            cpu.flags[Flag.ZERO.ordinal()] = (cpu.readByte(maybeZeroRegister) == 0x00);
        }

        switch (opFlagStrategy) {
            case SET:
                cpu.flags[Flag.OPERATION.ordinal()] = true;
                break;
            case RESET:
                cpu.flags[Flag.OPERATION.ordinal()] = false;
                break;
            case NONE:
                break;
        }
    }

    private enum FlagStrategy {
        SET,
        RESET,
        NONE;
    }
}