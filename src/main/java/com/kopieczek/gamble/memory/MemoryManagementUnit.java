package com.kopieczek.gamble.memory;

public class MemoryManagementUnit {
    public static final int BIOS_START       = 0x0000;
    public static final int BIOS_SIZE        = 0x0100;
    public static final int ROM_0_START      = 0x0000;
    public static final int ROM_0_SIZE       = 0x4000;
    public static final int ROM_1_START      = 0x4000;
    public static final int ROM_1_SIZE       = 0x4000;
    public static final int VRAM_START       = 0x8000;
    public static final int VRAM_SIZE        = 0x2000;
    public static final int EXT_RAM_START    = 0xa000;
    public static final int EXT_RAM_SIZE     = 0x2000;
    public static final int RAM_START        = 0xc000;
    public static final int RAM_SIZE         = 0x2000;
    public static final int SHADOW_RAM_START = 0xe000;
    public static final int SPRITES_START    = 0xfe00;
    public static final int SPRITES_SIZE     = 0x00a0;
    public static final int DEAD_AREA_START  = 0xfea0;
    // 0x60 bytes of unusable memory between sprites and IO.
    public static final int IO_AREA_START    = 0xff00;
    public static final int IO_AREA_SIZE     = 0x0080;
    public static final int ZRAM_START       = 0xff80;
    public static final int ZRAM_SIZE        = 0x0080;

    private final MemoryModule bios;
    private final MemoryModule rom0;
    private final MemoryModule rom1;
    private final MemoryModule gpuVram;
    private final MemoryModule extRam;
    private final MemoryModule ram;
    private final MemoryModule sprites;
    private final MemoryModule io;
    private final MemoryModule zram;

    private boolean shouldReadBios;

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
        shouldReadBios = true;
        validateMemoryModuleSizes();
    }

    private void validateMemoryModuleSizes() throws IllegalArgumentException {
        assertModuleSize("BIOS", bios, BIOS_SIZE);
        assertModuleSize("ROM0", rom0, ROM_0_SIZE);
        assertModuleSize("ROM1", rom1, ROM_1_SIZE);
        assertModuleSize("GPU VRAM", gpuVram, VRAM_SIZE);
        assertModuleSize("External RAM", extRam, EXT_RAM_SIZE);
        assertModuleSize("Working RAM", ram, RAM_SIZE);
        assertModuleSize("Sprite Space", sprites, SPRITES_SIZE);
        assertModuleSize("Memory-Mapped IO", io, IO_AREA_SIZE);
        assertModuleSize("High-Page RAM", zram, ZRAM_SIZE);
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
        if (address < ROM_1_START) {
            if (shouldReadBios && address < BIOS_START + BIOS_SIZE) {
                return bios.readByte(address - BIOS_START);
            } else {
                return rom0.readByte(address - ROM_0_START);
            }
        } else if (address < VRAM_START){
            return rom1.readByte(address - ROM_1_START);
        } else if (address < EXT_RAM_START) {
            return gpuVram.readByte(address - VRAM_START);
        } else if (address < RAM_START){
            return extRam.readByte(address - EXT_RAM_START);
        } else if (address < SHADOW_RAM_START) {
            return ram.readByte(address - RAM_START);
        } else if (address < SPRITES_START) {
            return ram.readByte(address - SHADOW_RAM_START);
        } else if (address < DEAD_AREA_START) {
            return sprites.readByte(address - SPRITES_START);
        } else if (address < IO_AREA_START) {
            throw new IllegalArgumentException("Cannot read from address " + address +
                    ": memory in range " + Integer.toHexString(DEAD_AREA_START) +
                    "-" + Integer.toHexString(IO_AREA_START - 1) +
                    " is inaccessible");
        } else if (address < ZRAM_START) {
            return io.readByte(address - IO_AREA_START);
        } else {
            return zram.readByte(address - ZRAM_START);
        }
    }

    public void setBiosEnabled(boolean isEnabled) {
        shouldReadBios = isEnabled;
    }

    public void setByte(int address, int value) {
        if (address < ROM_1_START) {
            if (shouldReadBios && address < BIOS_START + BIOS_SIZE) {
                bios.setByte(address - BIOS_START, value);
            } else {
                rom0.setByte(address - ROM_0_START, value);
            }
        } else if (address < VRAM_START){
            rom1.setByte(address - ROM_1_START, value);
        } else if (address < EXT_RAM_START) {
            gpuVram.setByte(address - VRAM_START, value);
        } else if (address < RAM_START){
            extRam.setByte(address - EXT_RAM_START, value);
        } else if (address < SHADOW_RAM_START) {
            ram.setByte(address - RAM_START, value);
        } else if (address < SPRITES_START) {
            ram.setByte(address - SHADOW_RAM_START, value);
        } else if (address < DEAD_AREA_START) {
            sprites.setByte(address - SPRITES_START, value);
        } else if (address < IO_AREA_START) {
            throw new IllegalArgumentException("Cannot write to address " + address +
                    ": memory in range " + Integer.toHexString(DEAD_AREA_START) +
                    "-" + Integer.toHexString(IO_AREA_START - 1) +
                    " is inaccessible");
        } else if (address < ZRAM_START) {
            io.setByte(address - IO_AREA_START, value);
        } else {
            zram.setByte(address - ZRAM_START, value);
        }
    }
}
