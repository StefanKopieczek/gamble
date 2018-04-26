package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;

abstract class GameCartridge implements Cartridge {
    private final MemoryModule rom0;
    private final MemoryModule rom1;
    private final MemoryModule ram;

    protected GameCartridge(int[] data) {
        rom0 = buildRom0(data);
        rom1 = buildRom1(data);
        ram = buildRam(data);
    }

    protected abstract MemoryModule buildRom0(final int[] data);

    protected abstract MemoryModule buildRom1(final int[] data);

    protected abstract MemoryModule buildRam(final int[] data);

    @Override
    public final MemoryModule getRom0() {
        return rom0;
    }

    @Override
    public final MemoryModule getRom1() {
        return rom1;
    }

    @Override
    public final MemoryModule getRam() {
        return ram;
    }
}
