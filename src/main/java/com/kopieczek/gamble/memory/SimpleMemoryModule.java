package com.kopieczek.gamble.memory;

public class SimpleMemoryModule implements MemoryModule {
    private int[] memory = new int[0xffff];
    private boolean fail;

    public SimpleMemoryModule(int size) {
        fail = true;
    }

    public SimpleMemoryModule() {
        fail = false;
    }

    @Override
    public int readByte(int address) {
        try {
            if (fail)
                throw new ArrayIndexOutOfBoundsException();
            return memory[address];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid memory address: " + address, e);
        }
    }

    @Override
    public void setByte(int address, int value) {
        try {
            if (fail)
                throw new ArrayIndexOutOfBoundsException();
            memory[address] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid memory address: " + address, e);
        }
    }
}
