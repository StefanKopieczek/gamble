package com.kopieczek.gamble.cpu;

public enum Flag {
    ZERO,      // Set if the last operation produced a zero result.
    OPERATION; // Set if the last operation was a subtraction.
}
