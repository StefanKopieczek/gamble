package com.kopieczek.gamble.hardware.cpu.timer;

import com.kopieczek.gamble.hardware.cpu.Interrupt;
import com.kopieczek.gamble.hardware.memory.InterruptLine;
import com.kopieczek.gamble.hardware.memory.TimerRegisters;

public class TimerChip {
    private static final int CYCLES_PER_DIV_TICK = 256;

    private final TimerRegisters registers;
    private final InterruptLine interrupts;
    private int ticks;

    public TimerChip(TimerRegisters registers, InterruptLine interrupts) {
        this.registers = registers;
        this.interrupts = interrupts;
    }

    public void tick(int clockCycles) {
        int cyclesPerCounterTick = registers.getCyclesPerTimerCounterTick();

        for (int i = 0; i < clockCycles; i++) {
            ticks++;
            if (ticks % CYCLES_PER_DIV_TICK == 0) {
                incrementDiv();
            }
            if (registers.isTimerEnabled() && (ticks % cyclesPerCounterTick == 0)) {
                incrementCounter();
            }
        }

        // No value of ticksPerCounter is slower than once per 1024 ticks,
        // and all possible values are factors of 1024.
        ticks %= 1024;
    }

    private void incrementDiv() {
        int oldDiv = registers.getTimerDiv();
        registers.setTimerDiv((oldDiv + 1) % 256);
    }

    private void incrementCounter() {
        int oldCounter = registers.getTimerCounter();
        if (oldCounter < 0xff) {
            registers.setTimerCounter(oldCounter + 1);
        } else {
            registers.resetTimerCounter();
            interrupts.setInterrupt(Interrupt.TIMER);
        }
    }
}
