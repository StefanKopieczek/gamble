package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.MemoryModule;

abstract class GameCartridge extends Cartridge {
    private final MemoryModule rom0;
    private final MemoryModule rom1;
    private final MemoryModule ram;
    private final int[] data;
    private String signature = null;

    protected GameCartridge(int[] data) {
        this.data = data;
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
    public final MemoryModule getRamInternal() {
        return ram;
    }

    @Override
    public String getSignature() {
        if (signature == null) {
            signature = this.getClass().getCanonicalName() + ":" + getDataDigest();
        }

        return signature;
    }

    private String getDataDigest() {
        final int prime = 31;
        long digest = 0;
        for (int datum : data) {
            digest += datum;
            digest = (digest * prime) % Integer.MAX_VALUE;
        }
        return Long.toString(digest);
    }
}
