package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public class Square1Channel extends SquareWaveChannel implements Square1RegisterListener {
    private final Io io;

    public Square1Channel(Io io) {
        super(io);
        this.io = io;
        io.register(this);
    }

    @Override
    protected AudioOutputMode getOutputMode() {
        return io.getSquare1OutputMode();
    }

    @Override
    protected short getVolume() {
        return (short)(io.getSquare1StartingVolume() * VOLUME_MULTIPLIER);
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
    }
}
