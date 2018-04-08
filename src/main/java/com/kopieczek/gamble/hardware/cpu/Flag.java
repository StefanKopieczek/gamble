package com.kopieczek.gamble.hardware.cpu;

public enum Flag {
    // Note that the CPU relies on the order of these flags to build the F register.
    CARRY,     // Set if the last operation caused byte rollover (except for INC).
    NIBBLE,    // Set if the last operation induced a carry of the less-significant nibble to the greater.
    OPERATION, // Set if the last operation was a subtraction.
    ZERO,      // Set if the last operation produced a zero result.
}