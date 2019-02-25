package com.kopieczek.gamble.hardware.memory.cartridge;

public interface ExtRamListener {
    void onRamChanged(int byteIndex, int value);
}
