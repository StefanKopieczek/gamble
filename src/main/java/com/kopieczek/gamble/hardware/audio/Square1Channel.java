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
    protected int getStepLengthInTicks() {
        int frequencyCounter = io.getSquare1FrequencyCounter();
        return 4 * (2048 - frequencyCounter);
    }

    @Override
    protected short getVolume() {
        return (short)(io.getSquare1StartingVolume() << 12);
    }

    @Override
    protected boolean[] getDutyCycle() {
        return io.getSquare1DutyCycle();
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
        // TODO
    }
}
