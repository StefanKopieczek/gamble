package com.kopieczek.gamble.hardware.memory;

import com.kopieczek.gamble.hardware.audio.Square1RegisterListener;

public class Square1RegisterAdapter implements Square1RegisterListener {
    @Override
    public void onLengthCounterUpdated(int newValue) {
        // Override me
    }

    @Override
    public void onTrigger() {
        // Override me
    }
}
