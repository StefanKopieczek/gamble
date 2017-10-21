package com.kopieczek.gamble.cpu;

import com.kopieczek.gamble.memory.IndirectAddress;

public enum Register {
    A, B, C, D, E, H, L;

    public static IndirectAddress BC = IndirectAddress.from(B, C);
    public static IndirectAddress HL = IndirectAddress.from(H, L);
}
