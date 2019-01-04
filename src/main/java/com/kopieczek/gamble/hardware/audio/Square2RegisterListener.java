package com.kopieczek.gamble.hardware.audio;

public interface Square2RegisterListener {
    void onLengthCounterUpdated(int newValue);
    void onTrigger();
}
