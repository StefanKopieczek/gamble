package com.kopieczek.gamble.hardware.memory;

import java.util.function.IntUnaryOperator;

public interface Filter extends IntUnaryOperator {

    /**
     * Returns a filter that prevents all changes to certain bits at the specified address.
     * @param module The module to which this filter will be applied (used to read existing value).
     * @param address The address to be protected
     * @param bitMask A bitmask, where high bits indicate the bits to be treated as read-only.
     * @return An instance of a filter meeting the given parameters
     */
    static Filter readOnlyFilter(MemoryModule module, int address, int bitMask) {
        return newValue -> {
            int oldValue = module.readByte(address);
            return (oldValue & bitMask) + (newValue & ~bitMask);
        };
    }
}
