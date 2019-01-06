package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.governor.Governor;

class VolumeEnvelope {
    // Envelope should tick at 64Hz
    private static final int CLOCK_DIVIDER = Governor.FREQUENCY_HZ / 64;

    private final int delta;
    private int tickDivider;
    private int currentVolume;
    private final int period;

    public VolumeEnvelope(int period, int startingVolume, int delta) {
        currentVolume = startingVolume;
        tickDivider = CLOCK_DIVIDER * period;
        this.period = period;
        this.delta = delta;
    }

    int tick() {
        tickDivider--;
        if (tickDivider == 0) {
            tickDivider = CLOCK_DIVIDER * period;
            currentVolume += delta;
            currentVolume = Math.max(0, currentVolume);
            currentVolume = Math.min(15, currentVolume);
        }

        return currentVolume;
    }
}
