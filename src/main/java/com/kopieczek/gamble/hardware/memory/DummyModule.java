package com.kopieczek.gamble.hardware.memory;

public class DummyModule implements MemoryModule {
    private final int size;

    public DummyModule(int size) {
        this.size = size;
    }
    @Override
    public int readByte(int address) {
        return 0xff;
    }

    @Override
    public void setByte(int address, int value) {
        // Do nothing
    }

    @Override
    public int getSizeInBytes() {
        return size;
    }
}
