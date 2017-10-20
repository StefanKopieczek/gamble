package com.kopieczek.gamble.memory;

import com.kopieczek.gamble.cpu.Register;

public class IndirectAddress {
    public final Register left;
    public final Register right;

    public static IndirectAddress from(Register left, Register right) {
        return new IndirectAddress(left, right);
    }

    private IndirectAddress(Register left, Register right) {
        this.left = left;
        this.right = right;
    }
}
