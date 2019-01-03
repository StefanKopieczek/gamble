package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public class Square2Channel extends SquareWaveChannel {
    private final Io io;

    public Square2Channel(Io io) {
        super(io);
        this.io = io;
    }

    @Override
    protected AudioOutputMode getOutputMode() {
        return io.getSquare2OutputMode();
    }

    @Override
    protected int getFrequency() {
        int frequencyCounter = io.getSquare2FrequencyCounter();
        return (int)((float)131072 / (2048 - frequencyCounter));
    }

    @Override
    protected short getVolume() {
        return (short)(io.getSquare2StartingVolume() << 12);
    }

    @Override
    protected boolean[] getDutyCycle() {
        return io.getSquare2DutyCycle();
    }

    @Override
    protected boolean isContinuousModeEnabled() {
        return io.isSquare2ContinuousModeEnabled();
    }
}
