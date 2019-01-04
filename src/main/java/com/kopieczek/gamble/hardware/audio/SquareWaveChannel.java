package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

abstract class SquareWaveChannel extends AbstractChannel{
    private static final int APU_TICKS_PER_COUNTER_TICK = Apu.MASTER_FREQUENCY_HZ / 256;

    private int tickCtr = 0;
    private int lengthCounter;

    SquareWaveChannel(Io io) {
        super(io);
    }

    @Override
    public final short getSample() {
        short sample;

        if (lengthCounter > 0 || isContinuousModeEnabled()) {
            int stepLengthInTicks = getStepLengthInTicks();
            boolean duty[] = getDutyCycle();
            short volume = getVolume();
            int periodLength = stepLengthInTicks * duty.length;
            int dutyOffset = (tickCtr % periodLength) / stepLengthInTicks;
            boolean isHigh = duty[dutyOffset];
            sample = isHigh ? volume : 0;
        } else {
            sample = 0;
        }

        tickCtr = (tickCtr + 1) % 32768;

        if (lengthCounter > 0 && tickCtr % APU_TICKS_PER_COUNTER_TICK == 0) {
            lengthCounter--;
        }

        return sample;
    }


    protected void updateLengthCounter(int newValue) {
        lengthCounter = newValue;
    }

    protected abstract int getStepLengthInTicks();
    protected abstract short getVolume();
    protected abstract boolean[] getDutyCycle();
    protected abstract boolean isContinuousModeEnabled();
}
