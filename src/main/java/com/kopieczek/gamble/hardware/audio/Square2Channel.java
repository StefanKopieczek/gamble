package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public class Square2Channel extends SquareWaveChannel implements Square2RegisterListener {
    private final Io io;

    public Square2Channel(Io io) {
        super(io);
        this.io = io;
        io.register((Square2RegisterListener)this);
    }

    @Override
    protected boolean isContinuousModeEnabled() {
        return io.isSquare2ContinuousModeEnabled();
    }

    @Override
    public void onLengthCounterUpdated(int newValue) {
        updateLengthCounter(newValue);
    }

    @Override
    public void onTrigger() {
        int frequencyCounter = io.getSquare2FrequencyCounter();
        int period = 4 * (2048 - frequencyCounter);
        updateFrequencyCounter(period);
        updateDuty(io.getSquare2DutyCycle());
        initVolumeEnvelope(io.getSquare2StartingVolume(), io.getSquare2EnvelopeStepLength(), io.getSquare2EnvelopeSign());
    }

    @Override
    public void onOutputModeChange(AudioOutputMode newOutputMode) {
        updateOutputMode(newOutputMode);
    }
}
