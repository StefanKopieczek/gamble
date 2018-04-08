package com.kopieczek.gamble.hardware.cpu;

public interface Word {
    int getValue(Cpu cpu);

    enum Register implements Word {
        AF(Byte.Register.A, Byte.Register.F),
        BC(Byte.Register.B, Byte.Register.C),
        DE(Byte.Register.D, Byte.Register.E),
        HL(Byte.Register.H, Byte.Register.L),
        SP(Byte.Register.S, Byte.Register.P);

        public final Byte.Register left;
        public final Byte.Register right;

        Register(Byte.Register left, Byte.Register right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int getValue(Cpu cpu) {
            return (left.getValue(cpu) << 8) + right.getValue(cpu);
        }
    }

    class Argument implements Word {
        private Integer value;

        @Override
        public int getValue(Cpu cpu) {
            if (value == null) {
                value = Byte.argument().getValue(cpu) + (Byte.argument().getValue(cpu) << 8);
            }
            return value;
        }
    }

    static Argument argument() {
        return new Argument();
    }

    class Literal implements Word {
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
