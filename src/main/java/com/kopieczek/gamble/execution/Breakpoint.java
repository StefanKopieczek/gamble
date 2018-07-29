package com.kopieczek.gamble.execution;

import com.kopieczek.gamble.hardware.cpu.Cpu;
import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.memory.Mmu;

public interface Breakpoint {
    boolean shouldBreak(Cpu cpu, Mmu mmu, Gpu gpu);
}
