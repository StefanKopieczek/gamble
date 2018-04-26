package com.kopieczek.gamble.hardware.memory.cartridge;

import com.kopieczek.gamble.hardware.memory.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CartridgeLoader {
    private static final Logger log = LogManager.getLogger(CartridgeLoader.class);
    private static final int MBC_TYPE_ADDR = 0x0147;

    public static Cartridge loadFrom(File f) throws IOException {
        log.info("Loading ROM from {}", f);
        return loadFrom(Files.readAllBytes(f.toPath()));
    }

    public static Cartridge loadFrom(byte[] data) {
        return loadFrom(mapToUnsigned(data));
    }

    public static Cartridge loadFrom(int[] data) {
        log.debug("Loading ROM from array of size {}", data.length);
        int mbcType = data[MBC_TYPE_ADDR];
        switch (mbcType) {
            case 0:
                return new MbcType0Cartridge(data);
            case 1:
                return new MbcType1Cartridge(data);
            default:
                throw new IllegalArgumentException("Unknown MBC type " + mbcType);
        }
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
}
