package com.kopieczek.gamble.hardware.memory;

public class RamModule extends MemoryModule {
    public static final int DEFAULT_SIZE = 0xffff;
    private int[] memory;

    public RamModule() {
        this(DEFAULT_SIZE);
    }

    public RamModule(int size) {
        super(size);
        memory = new int[size];
    }

    @Override
    public int readByte(int address) {
        try {
            return memory[address];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid memory address: " + address, e);
        }
    }

    @Override
    public void setByteDirect(int address, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Cannot loadPartial negative value to memory: " + value);
        } else if (value > 0xff) {
            throw new IllegalArgumentException("Cannot loadPartial overlarge value " + value + "; must fit in one byte");
        }

        try {
            memory[address] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid memory address: " + address, e);
        }
    }
}
