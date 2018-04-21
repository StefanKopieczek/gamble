package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.DummyModule;
import com.kopieczek.gamble.hardware.memory.MemoryModule;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.SimpleMemoryModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GameCartridge implements Cartridge {
    private static final Logger log = LogManager.getLogger(GameCartridge.class);
    private final int[] data;

    public GameCartridge(File f) throws IOException {
        this(Files.readAllBytes(f.toPath()));
        log.info("Loading ROM from {}", f);
    }

    public GameCartridge(int[] data) {
        log.debug("Loading ROM from array of size {}", data.length);
        this.data = data;
    }

    public GameCartridge(byte[] data) {
        log.debug("Loading ROM from array of size {}", data.length);
        this.data = new int[data.length];
        for (int ptr = 0; ptr < data.length; ptr++) {
            // Java bytes are signed; we want to use unsigned bytes
            // for the cartridge data so have to mask out the sign bit
            // and uplift to int.
            this.data[ptr] = 0xff & data[ptr];
        }
    }

    @Override
    public MemoryModule getRom0() {
        return new CartridgeModule(Mmu.ROM_0_START, Mmu.ROM_0_SIZE);
    }

    @Override
    public MemoryModule getRom1() {
        return new CartridgeModule(Mmu.ROM_1_START, Mmu.ROM_1_SIZE);
    }

    @Override
    public MemoryModule getRam() {
        // While nominally the RAM is stored on the cartridge, that's irrelevant for emulation purposes.
        return new SimpleMemoryModule(Mmu.EXT_RAM_SIZE);
    }

    private class CartridgeModule implements MemoryModule {
        private final int startAddr;
        private final int size;

        CartridgeModule(int startAddr, int size) {
            this.startAddr = startAddr;
            this.size = size;
        }

        @Override
        public int readByte(int address) {
            return data[startAddr + address];
        }

        @Override
        public void setByte(int address, int value) {
            // ROM is read only, so do nothing.
            log.warn("Program attempted to write to game ROM at address 0x{}", Integer.toHexString(address));
        }

        @Override
        public int getSizeInBytes() {
            return size;
        }
    }
}
