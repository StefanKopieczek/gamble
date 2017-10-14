package com.kopieczek.gamble.memory;

public class SimpleMemoryModule implements MemoryModule {
    private int[] memory = new int[0xffff];

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
        try {
            memory[address] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid memory address: " + address, e);
        }
    }
}
