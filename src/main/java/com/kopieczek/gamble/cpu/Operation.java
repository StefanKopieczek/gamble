package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.IndirectAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

enum Operation {
    NOP(0x00, FlagHandler.none(), (cpu) -> 4),
    LD_B(0x06, FlagHandler.none(), (cpu) -> {
        cpu.pc += 1;
        cpu.registers[Register.B.ordinal()] = cpu.readByte(cpu.pc);
        return 8;
    }),
    LD_C(0x0e, FlagHandler.none(), (cpu) -> {
        cpu.pc += 1;
        cpu.registers[Register.C.ordinal()] = cpu.readByte(cpu.pc);
        return 8;
    }),
    LD_D(0x16, FlagHandler.none(), (cpu) -> {
        cpu.pc += 1;
        cpu.registers[Register.D.ordinal()] = cpu.readByte(cpu.pc);
        return 8;
    }),
    LD_E(0x1e, FlagHandler.none(), (cpu) -> {
        cpu.pc += 1;
        cpu.registers[Register.E.ordinal()] = cpu.readByte(cpu.pc);
        return 8;
    }),
    LD_H(0x26, FlagHandler.none(), (cpu) -> {
        cpu.pc += 1;
        cpu.registers[Register.H.ordinal()] = cpu.readByte(cpu.pc);
        return 8;
    }),
    LD_L(0x2e, FlagHandler.none(), (cpu) -> {
        cpu.pc += 1;
        cpu.registers[Register.L.ordinal()] = cpu.readByte(cpu.pc);
        return 8;
    }),
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
            new FlagHandler().setZeroFlagIndirectlyFrom(Register.H, Register.L)
                    .setNibbleFlagIndirectlyFrom(Register.H, Register.L)
                    .opFlag(FlagHandler.Strategy.RESET),
            (cpu) -> {
                IndirectAddress address = IndirectAddress.from(Register.H, Register.L);
                cpu.increment(address);
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
        cpu.cycles += flagHandler.runWithFlagHandling(action, cpu);
    }

    private static class FlagHandler {
        private Register zeroFlagRegister = null;
        private Register nibbleFlagRegister = null;
        private IndirectAddress zeroFlagAddress = null;
        private IndirectAddress nibbleFlagAddress = null;
        private Strategy opFlagStrategy = Strategy.NONE;

        static FlagHandler none() {
            return new FlagHandler();
        }

        FlagHandler setZeroFlagFrom(Register r) {
            zeroFlagRegister = r;
            return this;
        }

        FlagHandler setZeroFlagIndirectlyFrom(Register left, Register right) {
            zeroFlagAddress = IndirectAddress.from(left, right);
            return this;
        }

        FlagHandler setNibbleFlagFrom(Register r) {
            nibbleFlagRegister = r;
            return this;
        }

        FlagHandler setNibbleFlagIndirectlyFrom(Register left, Register right) {
            nibbleFlagAddress = IndirectAddress.from(left, right);
            return this;
        }

        FlagHandler opFlag(Strategy strategy) {
            this.opFlagStrategy = strategy;
            return this;
        }

        public int runWithFlagHandling(Function<Cpu, Integer> action, Cpu cpu) {
            Integer before = null;
            if (nibbleFlagRegister != null) {
                before = cpu.readByte(nibbleFlagRegister) & 0x10;
            } else if (nibbleFlagAddress != null) {
                before = nibbleFlagAddress.getValueAt(cpu) & 0x10;
            }

            int cyclesUsed = action.apply(cpu);

            if (nibbleFlagRegister != null) {
                int after = cpu.readByte(nibbleFlagRegister) & 0x10;
                cpu.flags[Flag.NIBBLE.ordinal()] = (before < after);
            } else if (nibbleFlagAddress != null) {
                int after = nibbleFlagAddress.getValueAt(cpu) & 0x10;
                cpu.flags[Flag.NIBBLE.ordinal()] = (before < after);
            }

            if (zeroFlagRegister != null) {
                cpu.flags[Flag.ZERO.ordinal()] = (cpu.readByte(zeroFlagRegister) == 0x00);
            } else if (zeroFlagAddress != null) {
                cpu.flags[Flag.ZERO.ordinal()] = (zeroFlagAddress.getValueAt(cpu) == 0x00);
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

            return cyclesUsed;
        }

        public enum Strategy {
            SET,
            RESET,
            NONE
        }
    }
}