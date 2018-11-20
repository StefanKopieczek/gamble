package com.kopieczek.gamble.hardware.memory;

public interface TimerRegisters {
    boolean isTimerEnabled();
    int getCyclesPerTimerCounterTick();
    int getTimerDiv();
    void setTimerDiv(int newValue);
    int getTimerCounter();
    void setTimerCounter(int newValue);
    void resetTimerCounter();
}
