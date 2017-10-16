package com.kopieczek.gamble.cpu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

enum Operation {
    NOP(0x00, 4, FlagHandler.none(), (cpu) -> null),
    INC_A(0x3c, 4, new FlagHandler().setZeroFlagFrom(Register.A).opFlag(FlagHandler.Strategy.RESET), (cpu) -> {
        cpu.increment(Register.A);
        return null;
    }),
    INC_B(0x04, 4, new FlagHandler().setZeroFlagFrom(Register.B).opFlag(FlagHandler.Strategy.RESET), (cpu) -> {
        cpu.increment(Register.B);
        return null;
    }),
    INC_C(0x0c, 4, new FlagHandler().setZeroFlagFrom(Register.C).opFlag(FlagHandler.Strategy.RESET), (cpu) -> {
        cpu.increment(Register.C);
        return null;
    }),
    INC_D(0x14, 4, new FlagHandler().setZeroFlagFrom(Register.D).opFlag(FlagHandler.Strategy.RESET), (cpu) -> {
        cpu.increment(Register.D);
        return null;
    }),
    INC_E(0x1c, 4, new FlagHandler().setZeroFlagFrom(Register.E).opFlag(FlagHandler.Strategy.RESET), (cpu) -> {
        cpu.increment(Register.E);
        return null;
    }),
    INC_H(0x24, 4, new FlagHandler().setZeroFlagFrom(Register.H).opFlag(FlagHandler.Strategy.RESET), (cpu) -> {
        cpu.increment(Register.H);
        return null;
    }),
    INC_L(0x2c, 4, new FlagHandler().setZeroFlagFrom(Register.L).opFlag(FlagHandler.Strategy.RESET), (cpu) -> {
        cpu.increment(Register.L);
        return null;
    }),
    INC_HL(0x34, 12, new FlagHandler().setZeroFlagFrom(Register.H).opFlag(FlagHandler.Strategy.RESET), (cpu) -> {
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
    private FlagHandler flagHandler;
    int cycleCount;
    Function<Cpu, Void> action;

    Operation(int opcode, int cycleCount, FlagHandler flagHandler, Function<Cpu, Void> action) {
        this.opcode = opcode;
        this.flagHandler = flagHandler;
        this.cycleCount = cycleCount;
        this.action = action;
    }

    public void execute(Cpu cpu) {
        cpu.cycles += cycleCount;
        action.apply(cpu);
        if (flagHandler.getZeroFlagRegister() != null) {
            cpu.flags[Flag.ZERO.ordinal()] = (cpu.readByte(flagHandler.getZeroFlagRegister()) == 0x00);
        }

        switch (flagHandler.getOpFlagStrategy()) {
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

    private static class FlagHandler {
        private Register zeroFlagRegister = null;
        private Strategy strategy = Strategy.NONE;

        static FlagHandler none() {
            return new FlagHandler();
        }

        FlagHandler setZeroFlagFrom(Register r) {
            zeroFlagRegister = r;
            return this;
        }

        FlagHandler opFlag(Strategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Strategy getOpFlagStrategy() {
            return strategy;
        }

        public Register getZeroFlagRegister() {
            return zeroFlagRegister;
        }

        public static enum Strategy {
            SET,
            RESET,
            NONE
        }
    }
}