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
    private boolean[] duty = new boolean[8];

    SquareWaveChannel(Io io) {
        super(io);
    }

    @Override
    public final short getSample() {
        boolean expired = (lengthCounter == 0 && !isContinuousModeEnabled());
        if (expired) {
            // Disabled - return zero energy
            return 0;
        }

        lengthDivider = (lengthDivider == 0) ? APU_TICKS_PER_COUNTER_TICK : lengthDivider - 1;
        lengthCounter = (lengthDivider == 0) ? lengthCounter - 1 : lengthCounter;
        frequencyDivider = (frequencyDivider == 0) ? frequencyCounter : frequencyDivider - 1;

        if (frequencyDivider > 0) {
            // Haven't moved on from previous sample yet - re-emit it
            return lastValue;
        }

        short volume = getVolume();
        boolean isHigh = duty[dutyOffset];
        short sample = isHigh ? volume : 0;

        dutyOffset = (dutyOffset + 1) % 8;
        lastValue = sample;
        return sample;
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

    protected abstract short getVolume();
    protected abstract boolean isContinuousModeEnabled();
}
