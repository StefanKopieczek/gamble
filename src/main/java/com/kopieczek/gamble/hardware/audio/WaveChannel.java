package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public class WaveChannel extends AbstractChannel implements WaveRegisterListener {
    private static final int APU_TICKS_PER_COUNTER_TICK = Apu.MASTER_FREQUENCY_HZ / 256;

    private final Io io;
    private int tickCtr = 0;
    private int lengthCounter;

    WaveChannel(Io io) {
        super(io);
        this.io = io;
    }

    @Override
    public short getSample() {
        short sample;
        if (lengthCounter == 0 || io.isWaveContinuousModeEnabled()) {
            int stepLengthInTicks = getStepLengthInTicks();
            short samples[] = io.getWaveData();
            int waveformLength = stepLengthInTicks * samples.length;
            int waveformOffset = (tickCtr % waveformLength) / stepLengthInTicks;
            sample = (short) (samples[waveformOffset] * getVolume());
        } else {
            sample = 0;
        }

        tickCtr = (tickCtr + 1) % 32768;

        if (lengthCounter > 0 && tickCtr % APU_TICKS_PER_COUNTER_TICK == 0) {
            lengthCounter--;
        }
        return sample;
    }

    @Override
    protected AudioOutputMode getOutputMode() {
        return io.getWaveOutputMode();
    }

    private int getStepLengthInTicks() {
        int frequencyHz = getFrequency();
        float frequencyInTicks = (float)frequencyHz / Apu.MASTER_FREQUENCY_HZ;
        float stepFrequencyInTicks = frequencyInTicks * 32;
        return (int)(1 / stepFrequencyInTicks);
    }

    private int getFrequency() {
        int frequencyCounter = io.getWaveFrequencyCounter();
        return 4194304 / (64 * (2048 - frequencyCounter));
    }

    private short getVolume() {
        return (short)(((float)io.getWaveVolumePercent() / 1000) * Short.MAX_VALUE);
    }

    @Override
    public void onLengthCounterUpdated(int newValue) {
        lengthCounter = newValue;
    }
}
