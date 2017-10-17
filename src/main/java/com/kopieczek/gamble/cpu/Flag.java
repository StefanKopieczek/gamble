package com.kopieczek.gamble.cpu;

public enum Flag {
    ZERO,      // Set if the last operation produced a zero result.
    OPERATION, // Set if the last operation was a subtraction.
    NIBBLE,    // Set if the last operation induced a carry from the less-significant nibble to the greater.
    CARRY;     // Set if the last operation caused byte rollover (except for INC).
}
