package com.kopieczek.gamble.hardware.audio;

public class SimpleDecimator implements Downsampler {
    private long ctr;
    private int inputHz = 1;
    private int outputHz = 1;
    private long lastRemainder = -1;

    @Override
    public void setInputFrequency(int inputHz) {
        this.inputHz = inputHz;
    }

    @Override
    public void setOutputFrequency(int outputHz) {
        this.outputHz = outputHz;
    }

    @Override
    public int getInputFrequency() {
        return inputHz;
    }

    @Override
    public int getOutputFrequency() {
        return outputHz;
    }

    @Override
    public short[] accept(short[] sample) {
        long remainder = (long)(ctr * ((double)outputHz / inputHz));
        short[] output = null;
        if (remainder != lastRemainder) {
            output = sample;
            lastRemainder = remainder;
        }

        ctr++;
        return output;
    }
}
