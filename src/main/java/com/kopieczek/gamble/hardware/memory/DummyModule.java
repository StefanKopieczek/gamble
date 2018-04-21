package com.kopieczek.gamble.hardware.memory;

public class DummyModule extends MemoryModule {
    public DummyModule(int size) {
        super(size);
    }

    @Override
    public int readByte(int address) {
        return 0xff;
    }

    @Override
    public void setByteDirect(int address, int value) {
        // Do nothing
    }
}
