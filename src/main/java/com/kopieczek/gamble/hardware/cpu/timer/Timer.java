package com.kopieczek.gamble.hardware.cpu.timer;

import com.kopieczek.gamble.hardware.cpu.Interrupt;
import com.kopieczek.gamble.hardware.memory.InterruptLine;
import com.kopieczek.gamble.hardware.memory.TimerRegisters;

public class Timer {
    private static final int CYCLES_PER_DIV_TICK = 256;

    private final TimerRegisters registers;
    private final InterruptLine interrupts;
    private int ticks;

    public Timer(TimerRegisters registers, InterruptLine interrupts) {
        this.registers = registers;
        this.interrupts = interrupts;
    }

    public void tick(int clockCycles) {
        int cyclesPerCounterTick = registers.getCyclesPerTimerCounterTick();

        for (int i = 0; i < clockCycles; i++) {
            ticks = (ticks + 1) % 1024;
            if (ticks % CYCLES_PER_DIV_TICK == 0) {
                incrementDiv();
            }
            if (ticks % cyclesPerCounterTick == 0) {
                incrementCounter();
            }
        }
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
