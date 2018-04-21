package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;

interface MemoryBankController {
    void initControlBytes(MemoryModule module);
    int mapAddress(int busAddress);
}
