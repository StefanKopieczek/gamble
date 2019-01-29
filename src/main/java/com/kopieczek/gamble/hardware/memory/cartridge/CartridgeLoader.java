package com.kopieczek.gamble.hardware.memory.cartridge;

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
        int cartridgeType = data[MBC_TYPE_ADDR];
        switch (cartridgeType) {
            case 0: // ROM only
                return new MbcType0Cartridge(data);
            case 1: // MBC type 1
            case 2: // + RAM
            case 3: // + RAM and battery
                return new MbcType1Cartridge(data);
            // ------- Type 4 does not exist
            case 5: // MBC type 2
            case 6: // + RAM and battery
                unsupportedCartridge("MBC type 2");
            // ------- Type 7 does not exist
            case 8: // ROM + RAM
            case 9: // + battery
                return new MbcType0Cartridge(data);
            // ------- Type 10 does not exist
            case 11: // MMM01
            case 12: // + RAM
            case 13: // + RAM and battery
                unsupportedCartridge("MM01");
            // ------- Type 14 does not exist
            case 15: // MBC type 3 + timer + battery
            case 16: // MBC type 3 + RAM + timer + battery
            case 17: // MBC type 3
            case 18: // MBC type 3 + RAM
            case 19: // MBC type 3 + RAM + battery
                return new MbcType3Cartridge(data);
            // ------- Types 20-24 do not exist
            case 25: // MBC type 5
            case 26: // + RAM
            case 27: // + RAM + battery
            case 28: // + rumble
            case 29: // + RAM + rumble
            case 30: // + RAM + battery + rumble
                unsupportedCartridge("MBC type 5");
            // ------- Type 31 does not exist
            case 32: // MBC type 6 + RAM + battery
                unsupportedCartridge("MBC type 6");
            // ------- Type 33 does not exist
            case 34: // MBC type 7 + RAM + battery + accelerometer
                unsupportedCartridge("MBC type 7");
            // ------- Types 35-251 do not exist
            case 252: // GB Camera
                unsupportedCartridge("Gameboy Camera");
            case 253: // TAMA5
                unsupportedCartridge("Bandai TAMA5");
            case 254: // HuC3
                unsupportedCartridge("HuC3");
            case 255: // HuC1 + RAM + Battery
                unsupportedCartridge("HuC1");
            default:
                invalidCartridgeType(cartridgeType);
        }

        return null;
    }

    private static void invalidCartridgeType(int cartridgeType) {
        throw new IllegalArgumentException("Cartridge uses invalid cartridge type " + cartridgeType + ", which does not exist");
    }

    private static void unsupportedCartridge(String cartridgeType) {
        throw new UnsupportedOperationException("Cartridge uses " + cartridgeType + ", which is currently unsupported");
    }

    public static int getMbcTypeAddr() {
        return MBC_TYPE_ADDR;
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
