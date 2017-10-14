package com.kopieczek.gamble.memory;

public class SimpleMemoryModule implements MemoryModule {
    private int[] memory = new int[0xffff];

    @Override
    public int readByte(int address) {
        return memory[address];
    }

    @Override
    public void setByte(int address, int value) {
        memory[address] = value;
    }
}
