package com.kopieczek.gamble.hardware.memory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RomModule extends MemoryModule {
    private static final Logger log = LogManager.getLogger(RomModule.class);
    private final int[] data;

    public RomModule(int[] data) {
        super(data.length);
        this.data = data;
    }

    @Override
    public int readByte(int address) {
        return data[address];
    }

    @Override
    protected void setByteDirect(int address, int value) {
        log.warn("Program attempted to write to ROM in module {} at address {}", this, address);
    }
}
