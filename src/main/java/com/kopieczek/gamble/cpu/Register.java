package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.IndirectAddress;

public enum Register {
    A, B, C, D, E, H, L;

    public static final IndirectAddress BC = IndirectAddress.from(B, C);
    public static final IndirectAddress DE = IndirectAddress.from(D, E);
    public static final IndirectAddress HL = IndirectAddress.from(H, L);
}
