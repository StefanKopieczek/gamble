package com.kopieczek.gamble.cpu;

public class Pointer {
    Word address;

    private Pointer(Word address) {
        this.address = address;
    }

    public static Pointer of(Word word) {
        return new Pointer(word);
    }

    public static Pointer literal(int address) {
        return new Pointer(Word.literal(address));
    }

    void set(int value, Cpu cpu) {
        int addressLiteral = address.getValue(cpu);
        cpu.unsafeSet(addressLiteral, value);
    }

    int get(Cpu cpu) {
        return cpu.unsafeRead(address.getValue(cpu));
    }
}
