package com.kopieczek.gamble.hardware.memory;

import com.kopieczek.gamble.hardware.audio.NoiseRegisterListener;

public class NoiseRegisterAdapter implements NoiseRegisterListener {
    @Override
    public void onLengthCounterUpdated(int newValue) {
        // Override me
    }

    @Override
    public void onTrigger() {
        // Override me
    }
}
