package com.kopieczek.gamble.hardware.audio;

interface Filter {
    void setFrequency(int newValue);
    short filter(short sample);

    default double normalize(short in) {
        return ((double)in) / Short.MAX_VALUE;
    }

    default short denormalize(double out) {
        return (short)(out * Short.MAX_VALUE);
    }
}
