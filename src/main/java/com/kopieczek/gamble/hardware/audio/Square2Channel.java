package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public class Square2Channel extends SquareWaveChannel implements Square2RegisterListener {
    private final Io io;

    public Square2Channel(Io io) {
        super(io);
        this.io = io;
        io.register(this);
    }

    @Override
    protected AudioOutputMode getOutputMode() {
        return io.getSquare2OutputMode();
    }

    @Override
    protected short getVolume() {
        return (short)(io.getSquare2StartingVolume() * VOLUME_MULTIPLIER);
    }

    @Override
    protected boolean[] getDutyCycle() {
        return io.getSquare2DutyCycle();
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
    }
}
