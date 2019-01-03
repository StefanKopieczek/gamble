package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

abstract class SquareWaveChannel extends AbstractChannel{
    private int tickCtr = 0;

    SquareWaveChannel(Io io) {
        super(io);
    }

    @Override
    public final short getSample() {
        int stepLengthInTicks = getStepLengthInTicks();
        boolean duty[] = getDutyCycle();
        short volume = getVolume();
        int periodLength = stepLengthInTicks * duty.length;
        int dutyOffset = (tickCtr % periodLength) / stepLengthInTicks;
        boolean isHigh = duty[dutyOffset];
        short amplitude = isHigh ? volume : 0;
        tickCtr = (tickCtr + 1) % 32768;
        return amplitude;
    }

    private int getStepLengthInTicks() {
        int frequencyHz = getFrequency();
        float frequencyInTicks = (float)frequencyHz / Apu.MASTER_FREQUENCY_HZ;
        float stepFrequencyInTicks = frequencyInTicks * 8;
        return (int)(1 / stepFrequencyInTicks);
    }

    protected abstract int getFrequency();
    protected abstract short getVolume();
    protected abstract boolean[] getDutyCycle();
}
