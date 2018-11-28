package com.kopieczek.gamble.hardware.memory;

public class VramModule extends RamModule {
    VramModule() {
        super(Mmu.VRAM_SIZE);
    }
}
