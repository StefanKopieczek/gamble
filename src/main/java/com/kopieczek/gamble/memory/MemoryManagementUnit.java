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
        validateMemoryModuleSizes();
    }

    private void validateMemoryModuleSizes() throws IllegalArgumentException {
        assertModuleSize("BIOS", bios,0x100);
        assertModuleSize("ROM0", rom0,0x4000);
        assertModuleSize("ROM1", rom1,0x4000);
        assertModuleSize("GPU VRAM", gpuVram,0x2000);
        assertModuleSize("External RAM", extRam,0x2000);
        assertModuleSize("Working RAM", ram,0x2000);
        assertModuleSize("Sprite Space", sprites,0xa0);
        assertModuleSize("Memory-mapped IO", io,0x80);
        assertModuleSize("High-page RAM", zram,0x80);
    }

    private static void assertModuleSize(String moduleName, MemoryModule module, int expectedSize)
            throws IllegalArgumentException {
        if (module.getSizeInBytes() != expectedSize) {
            throw new IllegalArgumentException("Wrongly-sized " +
                    moduleName +
                    " module (was " +
                    module.getSizeInBytes() +
                    " bytes), expected " +
                    expectedSize +
                    " bytes)");
        }
    }

    public int readByte(int address) {
        return bios.readByte(address);
    }
}
