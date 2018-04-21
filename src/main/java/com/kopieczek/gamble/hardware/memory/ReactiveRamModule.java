package com.kopieczek.gamble.hardware.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

public class ReactiveRamModule extends SimpleRamModule {
    private Map<Integer, Trigger> triggers = new HashMap<>();
    private Map<Integer, Filter> filters = new HashMap<>();

    public ReactiveRamModule(int size) {
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

    public void setByteDirectly(int address, int value) {
        super.setByte(address, value);
    }

    public void addTrigger(int address, Trigger trigger) {
        triggers.put(address, trigger);
    }

    public void addFilter(int address, Filter filter) {
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

    public interface Trigger extends Runnable {}
    public interface Filter extends IntFunction<Integer> {}
}
