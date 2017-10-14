package com.kopieczek.gamble.memory;

public interface MemoryModule {
    int readByte(int address);
    void setByte(int i, int i1);
    int getSizeInBytes();
}
