package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;

public interface Cartridge {
    MemoryModule getRom0();
    MemoryModule getRom1();
    MemoryModule getRam();
}
