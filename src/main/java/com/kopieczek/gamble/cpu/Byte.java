package com.kopieczek.gamble.cpu;

public interface Byte {
    int getValue(Cpu cpu);

    enum Register implements Byte {
        A, B, C, D, E, H, L, // Standard byte registers.
        S, P,                // Byte parts of stack pointer.
        F,                   // Flag register: [Z, O, N, C, 0, 0, 0, 0]
        IE,                  // Interrupt enabled: [_, _, _, Joypad, Serial, Timer, LCD stat, V_BLANK]
        IF;                  // Interrupt flag:    [ ditto ]

        @Override
        public int getValue(Cpu cpu) {
            return cpu.registers[this.ordinal()];
        }
    }

    class Argument implements Byte {
        private Integer value;

        @Override
        public int getValue(Cpu cpu) {
            if (value == null) {
                value = cpu.readNextArg();
            }
            return value;
        }
    }

    static Argument argument() {
        return new Argument();
    }

    class Literal implements Byte {
        private int value;

        public Literal(int value) {
            this.value = value;
        }

        @Override
        public int getValue(Cpu cpu) {
            return value;
        }
    }

    static Literal literal(int value) {
        return new Literal(value);
    }
}
