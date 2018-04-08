package com.kopieczek.gamble.hardware.memory;

import com.kopieczek.gamble.hardware.cpu.Interrupt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

public class Mmu {
    private static final Logger log = LogManager.getLogger(Mmu.class);
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

    private static final int INTERRUPT_FLAG_ADDRESS = 0xff0f;

    private final MemoryModule bios;
    private final MemoryModule rom0;
    private final MemoryModule rom1;
    private final MemoryModule gpuVram;
    private final MemoryModule extRam;
    private final MemoryModule ram;
    private final MemoryModule sprites;
    private final IoModule io;
    private final MemoryModule zram;

    private boolean shouldReadBios;

    public Mmu(MemoryModule bios,
               MemoryModule rom0,
               MemoryModule rom1,
               MemoryModule gpuVram,
               MemoryModule extRam,
               MemoryModule ram,
               MemoryModule sprites,
               IoModule io,
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
        this.io.linkGlobalMemory(this);
        shouldReadBios = true;
        validateMemoryModuleSizes();
    }

    public static Mmu build() {
        return new Mmu(
                new BiosModule(),
                new SimpleMemoryModule(ROM_0_SIZE),
                new SimpleMemoryModule(ROM_1_SIZE),
                new SimpleMemoryModule(VRAM_SIZE),
                new SimpleMemoryModule(EXT_RAM_SIZE),
                new SimpleMemoryModule(RAM_SIZE),
                new SimpleMemoryModule(SPRITES_SIZE),
                new IoModule(),
                new SimpleMemoryModule(ZRAM_SIZE)
        );
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

    public void setBiosEnabled(boolean isEnabled) {
        shouldReadBios = isEnabled;
    }

    boolean isBiosEnabled() {
        return shouldReadBios;
    }

    public int readByte(int address) {
        MemoryModule module = getModuleForAddress(address);
        int localAddress = getLocalAddress(address, module);
        return module.readByte(localAddress);
    }

    public void setByte(int address, int value) {
        MemoryModule module = getModuleForAddress(address);
        int localAddress = getLocalAddress(address, module);
        module.setByte(localAddress, value);
    }

    private static int getLocalAddress(int globalAddress, MemoryModule module) {
        if ((module.getSizeInBytes() & 0xff) != 0 && globalAddress < ZRAM_START) {
            // Hack to handle the weirdly-located sprite area.
            return globalAddress & 0xff;
        } else {
            return globalAddress % module.getSizeInBytes();
        }
    }

    public MemoryModule getModuleForAddress(int globalAddress) {
        if (globalAddress < ROM_1_START) {
            if (shouldReadBios && globalAddress < BIOS_START + BIOS_SIZE) {
                return bios;
            } else {
                return rom0;
            }
        } else if (globalAddress < VRAM_START){
            return rom1;
        } else if (globalAddress < EXT_RAM_START) {
            return gpuVram;
        } else if (globalAddress < RAM_START){
            return extRam;
        } else if (globalAddress < SHADOW_RAM_START) {
            return ram;
        } else if (globalAddress < SPRITES_START) {
            return ram;
        } else if (globalAddress < DEAD_AREA_START) {
            return sprites;
        } else if (globalAddress < IO_AREA_START) {
            log.warn("Program attempted to access invalid address 0x" + Integer.toHexString(globalAddress));
            return new DummyModule(0x60);
        } else if (globalAddress < ZRAM_START) {
            return io;
        } else {
            return zram;
        }
    }

    public Io getIo() {
        return io;
    }

    public void setInterrupt(Interrupt interrupt) {
        log.debug("Interupt line {} fired", interrupt);
        final int currentValue = readByte(INTERRUPT_FLAG_ADDRESS);
        final int bitMask = (0x01 << interrupt.ordinal());
        setByte(INTERRUPT_FLAG_ADDRESS, currentValue | bitMask);
    }

    public boolean checkInterrupt(Interrupt interrupt) {
        final int flagValue = readByte(INTERRUPT_FLAG_ADDRESS);
        final int bitMask = (0x01 << interrupt.ordinal());
        return (flagValue & bitMask) > 0;
    }

    public void resetInterrupt(Interrupt interrupt) {
        log.trace("Interrupt {} reset by CPU", interrupt);
        final int currentValue = readByte(INTERRUPT_FLAG_ADDRESS);
        final int bitMask = ~(0x01 << interrupt.ordinal());
        setByte(INTERRUPT_FLAG_ADDRESS, currentValue & bitMask);
    }

    public int checkInterrupts() {
        return readByte(INTERRUPT_FLAG_ADDRESS);
    }

    public void loadRom(File f) throws IOException {
        byte[] data = Files.readAllBytes(f.toPath());
        log.info("Loading rom with {} bytes", data.length);
        for (int addr = 0; addr < data.length; addr++) {
            if (addr < 0x4000) {
                rom0.setByte(addr, 0xff & (int) data[addr]);
            } else {
                rom1.setByte(addr - 0x4000, 0xff & (int)data[addr]);
            }
        }
    }
}
