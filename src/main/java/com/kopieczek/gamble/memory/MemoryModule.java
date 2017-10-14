package com.kopieczek.gamble.memory;

public interface MemoryModule {
    int readByte(int address);
    void setByte(int address, int value);
    int getSizeInBytes();
}
