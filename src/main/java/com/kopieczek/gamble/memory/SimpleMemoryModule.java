package com.kopieczek.gamble.memory;

public class SimpleMemoryModule implements MemoryModule {
    public static final int DEFAULT_SIZE = 0xffff;
    private int[] memory;

    public SimpleMemoryModule() {
        this(DEFAULT_SIZE);
    }

    public SimpleMemoryModule(int size) {
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
    public void setByte(int address, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Cannot write negative value to memory: " + value);
        }

        try {
            memory[address] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid memory address: " + address, e);
        }
    }
}
