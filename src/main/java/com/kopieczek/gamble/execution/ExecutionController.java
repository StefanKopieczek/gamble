package com.kopieczek.gamble.execution;

import com.kopieczek.gamble.hardware.cpu.Cpu;
import com.kopieczek.gamble.hardware.governor.Governor;
import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.memory.Mmu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutionController {
    private final Cpu cpu;
    private final Mmu mmu;
    private final Gpu gpu;
    private final List<Breakpoint> breakpoints;
    private final List<ExecutionListener> listeners;
    private final AtomicBoolean isSuspended = new AtomicBoolean(false);

    public ExecutionController(Cpu cpu, Mmu mmu, Gpu gpu) {
        this.cpu = cpu;
        this.mmu = mmu;
        this.gpu = gpu;
        breakpoints = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public void runBlocking() {
        Governor governor = new Governor();
        while (true) {
            int cyclesBefore = cpu.getCycles();
            cpu.tick();
            int cycleDelta = cpu.getCycles() - cyclesBefore;
            mmu.stepAhead(cycleDelta);
            gpu.stepAhead(cycleDelta);
            governor.sleepIfNeeded(cycleDelta);
            if (shouldBreak()) {
                suspend();
            }
        }
    }

    private boolean shouldBreak() {
        synchronized (breakpoints) {
            return breakpoints.stream().anyMatch(breakpoint -> breakpoint.shouldBreak(cpu, mmu, gpu));
        }
    }

    private void suspend() {
        synchronized (listeners) {
            listeners.forEach(ExecutionListener::onBreak);
        }
        // TODO suspend
    }

    public void resume() {
        // TODO resume
    }

    public void addListener(ExecutionListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(ExecutionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void addBreakpoint(Breakpoint breakpoint) {
        synchronized (breakpoints) {
            breakpoints.add(breakpoint);
        }
    }

    public void removeBreakpoint(Breakpoint breakpoint) {
        synchronized (breakpoints) {
            breakpoints.remove(breakpoint);
        }
    }
}
