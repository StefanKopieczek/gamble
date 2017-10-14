package com.kopieczek.gamble.memory;

public class MemoryManagementUnit {
    private final MemoryModule bios;
    private final MemoryModule rom0;
    private final MemoryModule rom1;
    private final MemoryModule gpuVram;
    private final MemoryModule extRam;
    private final MemoryModule ram;
    private final MemoryModule sprites;
    private final MemoryModule io;
    private final MemoryModule zram;

    public MemoryManagementUnit(MemoryModule bios,
                                MemoryModule rom0,
                                MemoryModule rom1,
                                MemoryModule gpuVram,
                                MemoryModule extRam,
                                MemoryModule ram,
                                MemoryModule sprites,
                                MemoryModule io,
                                MemoryModule zram) {
        this.bios = bios;
        this.rom0 = rom0;
        this.rom1 = rom1;
        this.gpuVram = gpuVram;
        this.extRam = extRam;
        this.ram = ram;
        this.sprites = sprites;
        this.io = io;
        this.zram = zram;
    }
}
