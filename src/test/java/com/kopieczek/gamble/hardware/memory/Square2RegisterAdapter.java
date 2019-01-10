package com.kopieczek.gamble.hardware.memory;

import com.kopieczek.gamble.hardware.audio.AudioOutputMode;
import com.kopieczek.gamble.hardware.audio.Square2RegisterListener;

public class Square2RegisterAdapter implements Square2RegisterListener {
    @Override
    public void onLengthCounterUpdated(int newValue) {
        // Override me
    }

    @Override
    public void onTrigger() {
        // Override me
    }

    @Override
    public void onOutputModeChange(AudioOutputMode newOutputMode) {
        // Override me
    }
}
