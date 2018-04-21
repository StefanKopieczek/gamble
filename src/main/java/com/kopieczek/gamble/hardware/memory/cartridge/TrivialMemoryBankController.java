package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.ReactiveRamModule;

public class TrivialMemoryBankController implements MemoryBankController {
    @Override
    public void initControlBytes(ReactiveRamModule module) {
        // The trivial controller has no control bytes.
    }

    @Override
    public int mapAddress(int busAddress) {
        return busAddress;
    }
}