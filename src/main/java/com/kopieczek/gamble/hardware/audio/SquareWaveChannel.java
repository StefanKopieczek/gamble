package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.governor.Governor;
import com.kopieczek.gamble.hardware.memory.Io;

abstract class SquareWaveChannel extends AbstractChannel {
    private static final int APU_TICKS_PER_COUNTER_TICK = Governor.FREQUENCY_HZ / 256;
    static final int VOLUME_MULTIPLIER = Short.MAX_VALUE / 15;

    private int lengthCounter;
    private int frequencyCounter;
    private int frequencyDivider;
    private int lengthDivider = APU_TICKS_PER_COUNTER_TICK;
    private short lastValue;
    private int dutyOffset = 0;
    private boolean[] duty;
    private VolumeEnvelope volumeEnvelope;

    SquareWaveChannel(Io io) {
        super(io);
    }

    @Override
    public final short getSample() {
        boolean initialized = (duty != null);
        boolean expired = (lengthCounter == 0 && !isContinuousModeEnabled());
        if (!initialized || expired) {
            // Disabled - return zero energy
            return 0;
        }

        int x = volumeEnvelope.tick();
        short volume = (short)(VOLUME_MULTIPLIER * x);
//        System.err.println(x + " " + volume);

        lengthDivider = (lengthDivider == 0) ? APU_TICKS_PER_COUNTER_TICK : lengthDivider - 1;
        lengthCounter = (lengthDivider == 0) ? lengthCounter - 1 : lengthCounter;
        frequencyDivider = (frequencyDivider == 0) ? frequencyCounter : frequencyDivider - 1;

        if (frequencyDivider > 0) {
            // Haven't moved on from previous sample yet - re-emit it
            return (short)(lastValue * volume);
        }

        boolean isHigh = duty[dutyOffset];
        short sample = (short)(isHigh ? 1 : 0);

        dutyOffset = (dutyOffset + 1) % 8;
        lastValue = sample;
        return (short)(sample * volume);
    }


    protected void updateLengthCounter(int newValue) {
        lengthCounter = newValue;
    }

    protected void updateFrequencyCounter(int frequencyCounter) {
        this.frequencyCounter = frequencyCounter;
        this.frequencyDivider = frequencyCounter;
    }

    protected void updateDuty(boolean[] duty) {
        this.duty = duty;
        dutyOffset = 0;
    }

    protected void initVolumeEnvelope(int startingVolume, int period, int delta) {
        volumeEnvelope = new VolumeEnvelope(period, startingVolume, delta);
    }

    protected abstract boolean isContinuousModeEnabled();
}
