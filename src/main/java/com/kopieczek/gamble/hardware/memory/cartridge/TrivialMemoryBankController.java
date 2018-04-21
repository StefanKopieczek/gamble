package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;

public class TrivialMemoryBankController implements MemoryBankController {
    @Override
    public void initControlBytes(MemoryModule module) {
        // The trivial controller has no control bytes.
    }

    @Override
    public int mapAddress(int busAddress) {
        return busAddress;
    }
}