package com.kopieczek.gamble.hardware.memory;

import com.kopieczek.gamble.hardware.audio.WaveRegisterListener;

public class WaveRegisterAdapter implements WaveRegisterListener {
    @Override
    public void onLengthCounterUpdated(int newValue) {
        // Override me
    }

    @Override
    public void onTrigger() {
        // Override me
    }
}
