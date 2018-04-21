package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.ReactiveRamModule;

interface MemoryBankController {
    void initControlBytes(ReactiveRamModule module);
    int mapAddress(int busAddress);
}
