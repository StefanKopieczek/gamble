package com.kopieczek.gamble.hardware.audio;

public interface WaveRegisterListener {
    void onLengthCounterUpdated(int newValue);
    void onTrigger();
    void onOutputModeChange(AudioOutputMode newOutputMode);
}
