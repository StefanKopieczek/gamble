package com.kopieczek.gamble.execution;

import com.kopieczek.gamble.hardware.cpu.Cpu;
import com.kopieczek.gamble.hardware.governor.Governor;
import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.memory.Mmu;

public class ExecutionController {
    private final Cpu cpu;
    private final Mmu mmu;
    private final Gpu gpu;

    public ExecutionController(Cpu cpu, Mmu mmu, Gpu gpu) {
        this.cpu = cpu;
        this.mmu = mmu;
        this.gpu = gpu;
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
        }
    }
}
