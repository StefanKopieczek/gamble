package com.kopieczek.gamble.ui.debugger;

import com.kopieczek.gamble.hardware.cpu.Cpu;
import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.memory.Mmu;

import javax.swing.*;

public class DebuggerReadout extends JPanel {
    private final Cpu cpu;
    private final Mmu mmu;
    private final Gpu gpu;

    public DebuggerReadout(Cpu cpu, Mmu mmu, Gpu gpu) {
        super();
        this.cpu = cpu;
        this.mmu = mmu;
        this.gpu = gpu;
    }

    public void init() {
        add(new JLabel("<Debug info goes here>"));
    }
}
