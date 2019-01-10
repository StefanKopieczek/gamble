package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.governor.Governor;
import com.kopieczek.gamble.hardware.memory.Io;

public class WaveChannel extends AbstractChannel implements WaveRegisterListener {
    private static final int APU_TICKS_PER_COUNTER_TICK = Governor.FREQUENCY_HZ / 256;

    private final Io io;
    private int lengthCounter;
    private int frequencyDivider;
    private int lengthDivider = APU_TICKS_PER_COUNTER_TICK;
    private short lastValue;
    private int sampleOffset = 0;
    private short[] samples = new short[32];

    WaveChannel(Io io) {
        super(io);
        this.io = io;
        io.register((WaveRegisterListener)this);
    }

    @Override
    public short getSample() {
        boolean expired = (lengthCounter == 0 && !io.isWaveContinuousModeEnabled());
        if (expired || !io.isWaveDacEnabled()) {
            // Disabled - return zero energy
            return 0;
        }

        lengthDivider = (lengthDivider == 0) ? APU_TICKS_PER_COUNTER_TICK : lengthDivider - 1;
        lengthCounter = (lengthDivider == 0) ? lengthCounter - 1 : lengthCounter;
        frequencyDivider = (frequencyDivider == 0) ? getStepLengthInTicks() : frequencyDivider - 1;

        if (frequencyDivider > 0) {
            // Haven't moved on from previous sample yet - re-emit it
            return lastValue;
        }

        short volume = getVolume();
        short datum = samples[sampleOffset];
        short sample = (short)(datum * volume);

        sampleOffset = (sampleOffset + 1) % 32;
        lastValue = sample;
        return sample;
    }

    private int getStepLengthInTicks() {
        int frequencyCounter = io.getWaveFrequencyCounter();
        return 2 * (2048 - frequencyCounter);
    }

    private short getVolume() {
        return (short)(Short.MAX_VALUE * ((double)io.getWaveVolumePercent() / 1500));
    }

    @Override
    public void onLengthCounterUpdated(int newValue) {
        lengthCounter = newValue;
    }

    @Override
    public void onTrigger() {
        samples = io.getWaveData();
        sampleOffset = 0;
    }

    @Override
    public void onOutputModeChange(AudioOutputMode newOutputMode) {
        updateOutputMode(newOutputMode);
    }
}
