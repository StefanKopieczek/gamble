package com.kopieczek.gamble.memory;

import java.util.Map;

abstract class TriggeringMemoryModule extends SimpleMemoryModule {
    private final Map<Integer, Runnable> writeTriggers;

    TriggeringMemoryModule(int size) {
        super(size);
        writeTriggers = loadWriteTriggers();
    }

    @Override
    public void setByte(int address, int newValue) {
        super.setByte(address, newValue);
        if (writeTriggers.containsKey(address)) {
            writeTriggers.get(address).run();
        }
    }

    abstract Map<Integer, Runnable> loadWriteTriggers();
}
