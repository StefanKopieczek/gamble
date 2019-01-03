package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public class WaveChannel extends AbstractChannel {
    private final Io io;
    private int tickCtr = 0;

    WaveChannel(Io io) {
        super(io);
        this.io = io;
    }

    @Override
    public short getSample() {
        int stepLengthInTicks = getStepLengthInTicks();
        short samples[] = io.getWaveData();
        int sampleOffset = stepLengthInTicks * samples.length;
        int dutyOffset = (tickCtr % sampleOffset) / stepLengthInTicks;
        short sample = (short)(samples[dutyOffset] * getVolume());
        tickCtr = (tickCtr + 1) % 32768;
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
        return (short)(((float)io.getWaveVolumePercent() / 100) * Short.MAX_VALUE);
    }
}
