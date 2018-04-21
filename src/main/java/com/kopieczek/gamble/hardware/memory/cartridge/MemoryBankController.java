package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;

interface MemoryBankController {
    int mapAddress(int busAddress);
    MemoryModule getRam();
}
