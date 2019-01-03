package com.kopieczek.gamble.hardware.audio;

public interface Downsampler {
    void setInputFrequency(int inputHz);
    void setOutputFrequency(int outputHz);
    int getInputFrequency();
    int getOutputFrequency();
    short[] accept(short[] sample);
}
