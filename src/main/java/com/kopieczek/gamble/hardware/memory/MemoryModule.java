package com.kopieczek.gamble.hardware.memory;

public interface MemoryModule {
    int readByte(int address);
    void setByte(int address, int value);
    int getSizeInBytes();
}
