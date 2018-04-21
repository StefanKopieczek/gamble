package com.kopieczek.gamble.hardware.memory;

import java.util.HashMap;
import java.util.Map;

public abstract class MemoryModule implements Memory {
    private final int size;
    private final Map<Integer, Filter> filters = new HashMap<>();
    private final Map<Integer, Trigger> triggers = new HashMap<>();

    public MemoryModule(int size) {
        this.size = size;
    }

    @Override
    public abstract int readByte(int address);

    protected abstract void setByteDirect(int address, int value);

    @Override
    public void setByte(int address, int value) {
        if (filters.containsKey(address)) {
            value = filters.get(address).applyAsInt(value);
        }

        setByteDirect(address, value);

        if (triggers.containsKey(address)) {
            triggers.get(address).run();
        }
    }

    public void addFilter(int address, Filter f) {
        filters.put(address, f);
    }

    public void addTrigger(int address, Trigger t) {
        triggers.put(address, t);
    }

    public int getSizeInBytes() {
        return size;
    }
}
