package com.kopieczek.gamble.memory;

public class SimpleMemoryModule implements MemoryModule {
    private int value = 0x0;

    @Override
    public int readByte(int address) {
        return value;
    }

    @Override
    public void setByte(int i, int i1) {
        value = 0xff;
    }
}
