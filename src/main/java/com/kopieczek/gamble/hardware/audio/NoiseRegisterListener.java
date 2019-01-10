package com.kopieczek.gamble.hardware.audio;

public interface NoiseRegisterListener {
    void onLengthCounterUpdated(int newValue);
    void onTrigger();
    void onOutputModeChange(AudioOutputMode newOutputMode);
}
