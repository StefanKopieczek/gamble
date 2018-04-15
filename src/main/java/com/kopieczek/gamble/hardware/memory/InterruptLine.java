package com.kopieczek.gamble.hardware.memory;

import com.kopieczek.gamble.hardware.cpu.Interrupt;

public interface InterruptLine {
    void setInterrupt(Interrupt interrupt);

    boolean checkInterrupt(Interrupt interrupt);

    void resetInterrupt(Interrupt interrupt);

    int checkInterrupts();
}
