package com.kopieczek.gamble.hardware.memory;

public interface Memory {
    int readByte(int address);
    void setByte(int address, int value);
}
