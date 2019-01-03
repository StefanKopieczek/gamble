package com.kopieczek.gamble.hardware.memory;

import com.kopieczek.gamble.hardware.audio.Square2RegisterListener;

public class Square2RegisterAdapter implements Square2RegisterListener {
    @Override
    public void onLengthCounterUpdated(int newValue) {
        // Override me
    }
}
