package com.kopieczek.gamble.cpu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

enum Operation {
    NOP(0x00, FlagHandler.none(), (cpu) -> 4),
    INC_A(0x3c,
          new FlagHandler().setZeroFlagFrom(Register.A)
                           .setNibbleFlagFrom(Register.A)
                           .opFlag(FlagHandler.Strategy.RESET),
          (cpu) -> {
              cpu.increment(Register.A);
              return 4;
          }
    ),
    INC_B(0x04,
          new FlagHandler().setZeroFlagFrom(Register.B)
                           .setNibbleFlagFrom(Register.B)
                           .opFlag(FlagHandler.Strategy.RESET),
          (cpu) -> {
              cpu.increment(Register.B);
              return 4;
          }
    ),
    INC_C(0x0c,
          new FlagHandler().setZeroFlagFrom(Register.C)
                           .setNibbleFlagFrom(Register.C)
                           .opFlag(FlagHandler.Strategy.RESET),
          (cpu) -> {
              cpu.increment(Register.C);
              return 4;
          }
    ),
    INC_D(0x14,
          new FlagHandler().setZeroFlagFrom(Register.D)
                           .setNibbleFlagFrom(Register.D)
                           .opFlag(FlagHandler.Strategy.RESET),
          (cpu) -> {
              cpu.increment(Register.D);
              return 4;
          }
    ),
    INC_E(0x1c,
          new FlagHandler().setZeroFlagFrom(Register.E)
                           .setNibbleFlagFrom(Register.E)
                           .opFlag(FlagHandler.Strategy.RESET),
          (cpu) -> {
              cpu.increment(Register.E);
              return 4;
          }
    ),
    INC_H(0x24,
          new FlagHandler().setZeroFlagFrom(Register.H)
                           .setNibbleFlagFrom(Register.H)
                           .opFlag(FlagHandler.Strategy.RESET),
          (cpu) -> {
              cpu.increment(Register.H);
              return 4;
          }
    ),
    INC_L(0x2c,
          new FlagHandler().setZeroFlagFrom(Register.L)
                           .setNibbleFlagFrom(Register.L)
                           .opFlag(FlagHandler.Strategy.RESET),
          (cpu) -> {
              cpu.increment(Register.L);
              return 4;
          }
    ),
    INC_HL(0x34,
            new FlagHandler().setZeroFlagFrom(Register.H)
                    .setNibbleFlagFrom(Register.L)
                    .opFlag(FlagHandler.Strategy.RESET),
            (cpu) -> {
                cpu.increment(Register.L);
                if (cpu.readByte(Register.L) == 0) {
                    cpu.increment(Register.H);
                }
                return 12;
            }
    );

    static final Map<Integer, Operation> map = new HashMap<>();
    static {
        for (Operation op : Operation.values()) {
            map.put(op.opcode, op);
        }
    }

    private int opcode;
    private FlagHandler flagHandler;
    Function<Cpu, Integer> action;

    Operation(int opcode, FlagHandler flagHandler, Function<Cpu, Integer> action) {
        this.opcode = opcode;
        this.flagHandler = flagHandler;
        this.action = action;
    }

    public void execute(Cpu cpu) {
        Register nibbleRegister = flagHandler.getNibbleFlagRegister();
        Integer nibbleRegisterBefore = null;
        if (nibbleRegister != null) {
            nibbleRegisterBefore = cpu.readByte(nibbleRegister);
        }

        cpu.cycles += action.apply(cpu);

        if (nibbleRegister != null) {
            int nibbleRegisterAfter = cpu.readByte(nibbleRegister);
            cpu.flags[Flag.NIBBLE.ordinal()] = (nibbleRegisterAfter != nibbleRegisterBefore);
        }

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
        private Register nibbleFlagRegister = null;
        private Strategy strategy = Strategy.NONE;

        static FlagHandler none() {
            return new FlagHandler();
        }

        FlagHandler setZeroFlagFrom(Register r) {
            zeroFlagRegister = r;
            return this;
        }

        FlagHandler setNibbleFlagFrom(Register r) {
            nibbleFlagRegister = r;
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

        public Register getNibbleFlagRegister() {
            return nibbleFlagRegister;
        }

        public static enum Strategy {
            SET,
            RESET,
            NONE
        }
    }
}