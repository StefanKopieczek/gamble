package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.execution.ExecutionController;
import com.kopieczek.gamble.hardware.cpu.Cpu;
import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.ui.debugger.DebuggerControls;
import com.kopieczek.gamble.ui.debugger.DebuggerReadout;

import javax.swing.*;
import java.awt.*;

public class DebuggerUi extends Ui {
    private final Cpu cpu;
    private final Mmu mmu;
    private final Gpu gpu;
    private final ExecutionController controller;

    public DebuggerUi(Cpu cpu, Mmu mmu, Gpu gpu, ExecutionController controller) {
        super("Gamble (Debug mode)");
        this.cpu = cpu;
        this.mmu = mmu;
        this.gpu = gpu;
        this.controller = controller;
    }

    @Override
    protected void setupUi() {
        Screen screen = new Screen(gpu.getScreenBuffer());
        screen.init();
        getContentPane().add(screen, BorderLayout.CENTER);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BorderLayout());

        Controls controls = new Controls(mmu.getIo());
        controls.init();
        southPanel.add(controls, BorderLayout.NORTH);

        southPanel.add(buildDebugView(), BorderLayout.CENTER);
        southPanel.add(buildDebugControls(), BorderLayout.SOUTH);

        getContentPane().add(southPanel, BorderLayout.SOUTH);
    }

    private JPanel buildDebugView() {
        DebuggerReadout readout = new DebuggerReadout(cpu, mmu, gpu);
        readout.init();
        return readout;
    }

    private JPanel buildDebugControls() {
        DebuggerControls debugControls = new DebuggerControls(controller);
        debugControls.init();
        return debugControls;
    }
}
