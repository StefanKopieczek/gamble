package com.kopieczek.gamble.hardware.audio;

import com.kopieczek.gamble.hardware.memory.Io;

public class Square1Channel extends SquareWaveChannel {
    private final Io io;

    public Square1Channel(Io io) {
        this.io = io;
    }

    @Override
    protected int getFrequency() {
        int frequencyCounter = io.getSquare1FrequencyCounter();
        return (int)((float)131072 / (2048 - frequencyCounter));
    }

    @Override
    protected short getVolume() {
        return (short)(io.getSquare1StartingVolume() << 12);
    }

    @Override
    protected boolean[] getDutyCycle() {
        return io.getSquare1DutyCycle();
    }
}
