package com.kopieczek.gamble.hardware.audio;

public class SimpleDecimator implements Downsampler {
    private int ctr;
    private int inputHz = 1;
    private int outputHz = 1;
    private int modulus;

    @Override
    public void setInputFrequency(int inputHz) {
        this.inputHz = inputHz;
        modulus = inputHz / outputHz;
    }

    @Override
    public void setOutputFrequency(int outputHz) {
        this.outputHz = outputHz;
        modulus = inputHz / outputHz;
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
        boolean shouldOutput = (ctr == 0);
        ctr = (ctr + 1) % modulus;
        return shouldOutput ? sample : null;
    }
}
