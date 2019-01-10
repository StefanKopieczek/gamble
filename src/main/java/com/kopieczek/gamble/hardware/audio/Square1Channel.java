package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public class Square1Channel extends SquareWaveChannel implements Square1RegisterListener {
    private final Io io;

    public Square1Channel(Io io) {
        super(io);
        this.io = io;
        io.register((Square1RegisterListener)this);
    }

    @Override
    protected boolean isContinuousModeEnabled() {
        return io.isSquare1ContinuousModeEnabled();
    }

    @Override
    public void onLengthCounterUpdated(int newValue) {
        updateLengthCounter(newValue);
    }

    @Override
    public void onTrigger() {
        int frequencyCounter = io.getSquare1FrequencyCounter();
        updateFrequencyCounter(4 * (2048 - frequencyCounter));
        updateDuty(io.getSquare1DutyCycle());
        initVolumeEnvelope(io.getSquare1StartingVolume(), io.getSquare1EnvelopeStepLength(), io.getSquare1EnvelopeSign());
    }

    @Override
    public void onOutputModeChange(AudioOutputMode newOutputMode) {
        updateOutputMode(newOutputMode);
    }
}
