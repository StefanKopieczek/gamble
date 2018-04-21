package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GameCartridge implements Cartridge {
    private static final Logger log = LogManager.getLogger(GameCartridge.class);
    private final int[] data;
    private final MemoryBankController mbc;
    private final MemoryModule rom0;
    private final MemoryModule rom1;

    public GameCartridge(File f) throws IOException {
        this(Files.readAllBytes(f.toPath()));
        log.info("Loading ROM from {}", f);
    }

    public GameCartridge(byte[] data) {
        this(mapToUnsigned(data));
    }

    public GameCartridge(int[] data) {
        log.debug("Loading ROM from array of size {}", data.length);
        this.data = data;
        mbc = initMbc();
        rom0 = new FixedRom(Mmu.ROM_0_START, Mmu.ROM_0_SIZE);
        rom1 = new BankedRom(Mmu.ROM_1_START, Mmu.ROM_1_SIZE);
    }

    private MemoryBankController initMbc() {
        return new TrivialMemoryBankController();
    }

    private static int[] mapToUnsigned(byte[] data) {
        int[] unsigned = new int[data.length];
        for (int ptr = 0; ptr < data.length; ptr++) {
            // Java bytes are signed; we want to use unsigned bytes
            // for the cartridge data so have to mask out the sign bit
            // and uplift to int.
            unsigned[ptr] = 0xff & data[ptr];
        }

        return unsigned;
    }

    @Override
    public MemoryModule getRom0() {
        return rom0;
    }

    @Override
    public MemoryModule getRom1() {
        return rom1;
    }

    @Override
    public MemoryModule getRam() {
        // While nominally the RAM is stored on the cartridge, that's irrelevant for emulation purposes.
        return mbc.getRam();
    }

    private class FixedRom extends MemoryModule {
        private final int startAddr;

        FixedRom(int startAddr, int size) {
            super(size);
            this.startAddr = startAddr;
        }

        @Override
        public int readByte(int address) {
            return data[startAddr + address];
        }

        @Override
        public void setByteDirect(int address, int value) {
            // ROM is read only, so do nothing.
            log.warn("Program attempted to write to game ROM at address 0x{}", Integer.toHexString(address));
        }
    }

    private class BankedRom extends MemoryModule {
        private final int startAddr;

        BankedRom(int startAddr, int size) {
            super(size);
            this.startAddr = startAddr;
        }

        @Override
        public int readByte(int address) {
            int busAddr = startAddr + address;
            int arrayPtr = mbc.mapAddress(busAddr);
            return data[arrayPtr];
        }

        @Override
        public void setByteDirect(int address, int value) {
            // ROM is read only, so do nothing.
            log.warn("Program attempted to write to game ROM at address 0x{}", Integer.toHexString(address));
        }
    }
}
