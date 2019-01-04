package com.kopieczek.gamble.hardware.audio;

public interface Square1RegisterListener {
    void onLengthCounterUpdated(int newValue);
    void onTrigger();
}
