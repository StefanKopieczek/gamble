package com.kopieczek.gamble.cpu;

public interface Byte {
    int getValue(Cpu cpu);

    enum Register implements Byte {
        A, B, C, D, E, H, L;

        @Override
        public int getValue(Cpu cpu) {
            return cpu.registers[this.ordinal()];
        }
    }

    class Argument implements Byte {
        @Override
        public int getValue(Cpu cpu) {
            return cpu.readNextArg();
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
