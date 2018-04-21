package com.kopieczek.gamble.hardware.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

class ReactiveMemoryModule extends SimpleMemoryModule {
    private Map<Integer, Trigger> triggers = new HashMap<>();
    private Map<Integer, Filter> filters = new HashMap<>();

    ReactiveMemoryModule(int size) {
        super(size);
    }

    @Override
    public void setByte(int address, int value) {
        if (filters.containsKey(address)) {
            value = filters.get(address).apply(value);
        }

        super.setByte(address, value);

        if (triggers.containsKey(address)) {
            triggers.get(address).run();
        }
    }

    void setByteDirectly(int address, int value) {
        super.setByte(address, value);
    }

    void addTrigger(int address, Trigger trigger) {
        triggers.put(address, trigger);
    }

    void addFilter(int address, Filter filter) {
        filters.put(address, filter);
    }

    /**
     * Prevents modification to specified bits
     * @param address The address to be protected
     * @param bitMask A bit mask indicating which bits can be written to - high bits will be read only.
     */
    Filter readOnlyBitsFilter(int address, int bitMask) {
        return proposedValue -> ((readByte(address) & bitMask) + (proposedValue & ~bitMask));
    }

    interface Trigger extends Runnable {}
    interface Filter extends IntFunction<Integer> {}
}
