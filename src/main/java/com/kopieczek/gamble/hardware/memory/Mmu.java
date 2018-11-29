package com.kopieczek.gamble.hardware.memory;

import com.kopieczek.gamble.hardware.cpu.Interrupt;
import com.kopieczek.gamble.hardware.memory.cartridge.Cartridge;
import com.kopieczek.gamble.hardware.memory.cartridge.EmptyCartridge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

public class Mmu implements Memory, InterruptLine, GraphicsAccessController {
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
    public static final int OAM_START        = 0xfe00;
    public static final int OAM_SIZE         = 0x00a0;
    public static final int DEAD_AREA_START  = 0xfea0;
    // 0x60 bytes of unusable memory between oam and IO.
    public static final int IO_AREA_START    = 0xff00;
    public static final int IO_AREA_SIZE     = 0x0080;
    public static final int ZRAM_START       = 0xff80;
    public static final int ZRAM_SIZE        = 0x0080;

    private static final int INTERRUPT_FLAG_ADDRESS = 0xff0f;

    private final MemoryModule bios;
    private MemoryModule rom0;
    private MemoryModule rom1;
    private final VramModule vram;
    private MemoryModule extRam;
    private final MemoryModule ram;
    private final OamModule oam;
    private final IoModule io;
    private final MemoryModule zram;

    private boolean shouldReadBios;
    private boolean isVramAccessible = true;
    private boolean isOamAccessible = true;
    private List<DmaProcess> ongoingDmas = new LinkedList<DmaProcess>();

    Mmu(MemoryModule bios,
               Cartridge cartridge,
               VramModule vram,
               MemoryModule ram,
               OamModule oam,
               IoModule io,
               MemoryModule zram) {
        this(bios, cartridge.getRom0(), cartridge.getRom1(), vram, cartridge.getRam(), ram, oam, io, zram);
    }

    Mmu(MemoryModule bios,
               MemoryModule rom0,
               MemoryModule rom1,
               VramModule vram,
               MemoryModule extRam,
               MemoryModule ram,
               OamModule oam,
               IoModule io,
               MemoryModule zram) {
        this.bios = bios;
        this.rom0 = rom0;
        this.rom1 = rom1;
        this.vram = vram;
        this.extRam = extRam;
        this.ram = ram;
        this.oam = oam;
        this.io = io;
        this.zram = zram;
        this.io.linkGlobalMemory(this);
        shouldReadBios = true;
        validateMemoryModuleSizes();
    }

    public void stepAhead(int cycles) {
        // MMU clock is only used to correctly time ongoing DMAs.
        ongoingDmas.forEach(dma -> dma.tick(this, cycles));
        ongoingDmas.removeIf(DmaProcess::isFinished);
    }

    public static Mmu build(boolean skipBios) {
        Cartridge cartridge = new EmptyCartridge();
        RomModule bios = (skipBios) ? new FastBiosModule() : new BiosModule();
        return new Mmu(
                bios,
                cartridge,
                new VramModule(),
                new RamModule(RAM_SIZE),
                new OamModule(),
                new IoModule(),
                new RamModule(ZRAM_SIZE)
        );
    }

    private void validateMemoryModuleSizes() throws IllegalArgumentException {
        assertModuleSize("BIOS", bios, BIOS_SIZE);
        assertModuleSize("ROM0", rom0, ROM_0_SIZE);
        assertModuleSize("ROM1", rom1, ROM_1_SIZE);
        assertModuleSize("GPU VRAM", vram, VRAM_SIZE);
        assertModuleSize("External RAM", extRam, EXT_RAM_SIZE);
        assertModuleSize("Working RAM", ram, RAM_SIZE);
        assertModuleSize("Sprite Space", oam, OAM_SIZE);
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

    public Memory getDirectMemoryAccess() {
        return this;
    }

    public Memory getShieldedMemoryAccess() {
        return new Memory() {
            @Override
            public int readByte(int address) {
                if (isAccessible(address)) {
                    return Mmu.this.readByte(address);
                } else {
                    log.warn("Program tried to read from address {} while it was read only",
                        Integer.toHexString(address));
                    return 0xff;
                }
            }

            @Override
            public void setByte(int address, int value) {
                if (isAccessible(address)) {
                    Mmu.this.setByte(address, value);
                } else {
                    log.warn("Program tried to write to address {} while it was read only",
                        Integer.toHexString(address));
                }
            }

            private boolean isAccessible(int address) {
                MemoryModule module = getModuleForAddress(address);
/*                if (ongoingDmas.size() > 0) {
                    return module == zram;
                } else */if (module == vram) {
                    return isVramAccessible;
                } else if (module == oam) {
                    return isOamAccessible;
                } else {
                    return true;
                }
            }
        };
    }

    public InterruptLine getInterruptLine() {
        return this;
    }

    public GraphicsAccessController getGraphicsAccessController() {
        return this;
    }

    @Override
    public int readByte(int address) {
        MemoryModule module = getModuleForAddress(address);
        int localAddress = getLocalAddress(address, module);
        return module.readByte(localAddress);
    }

    @Override
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
            return vram;
        } else if (globalAddress < RAM_START){
            return extRam;
        } else if (globalAddress < SHADOW_RAM_START) {
            return ram;
        } else if (globalAddress < OAM_START) {
            return ram;
        } else if (globalAddress < DEAD_AREA_START) {
            return oam;
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

    @Override
    public void setInterrupt(Interrupt interrupt) {
        log.debug("Interupt line {} fired", interrupt);
        final int currentValue = readByte(INTERRUPT_FLAG_ADDRESS);
        final int bitMask = (0x01 << interrupt.ordinal());
        setByte(INTERRUPT_FLAG_ADDRESS, currentValue | bitMask);
    }

    @Override
    public boolean checkInterrupt(Interrupt interrupt) {
        final int flagValue = readByte(INTERRUPT_FLAG_ADDRESS);
        final int bitMask = (0x01 << interrupt.ordinal());
        return (flagValue & bitMask) > 0;
    }

    @Override
    public void resetInterrupt(Interrupt interrupt) {
        log.trace("Interrupt {} reset by CPU", interrupt);
        final int currentValue = readByte(INTERRUPT_FLAG_ADDRESS);
        final int bitMask = ~(0x01 << interrupt.ordinal());
        setByte(INTERRUPT_FLAG_ADDRESS, currentValue & bitMask);
    }

    @Override
    public int checkInterrupts() {
        return readByte(INTERRUPT_FLAG_ADDRESS);
    }

    public void loadCartridge(Cartridge cartridge) {
        rom0 = cartridge.getRom0();
        rom1 = cartridge.getRom1();
        extRam = cartridge.getRam();
        validateMemoryModuleSizes();
    }

    void doDmaTransfer(int startIndicator) {
        int startAddress = startIndicator << 8;
        ongoingDmas.add(new DmaProcess(startAddress, OAM_START));
    }

    @Override
    public void setVramAccessible(boolean isAccessible) {
        isVramAccessible = isAccessible;
    }

    @Override
    public void setOamAccessible(boolean isAccessible) {
        isOamAccessible = isAccessible;
    }

    public Oam getOam() {
        return oam;
    }

    public Vram getVram() {
        return vram;
    }
}

