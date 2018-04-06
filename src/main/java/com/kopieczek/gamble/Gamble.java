package com.kopieczek.gamble;

import com.kopieczek.gamble.cpu.Cpu;
import com.kopieczek.gamble.graphics.Gpu;
import com.kopieczek.gamble.memory.MemoryManagementUnit;
import com.kopieczek.gamble.ui.GambleUi;

import javax.swing.*;

public class Gamble {
    public static void main(String[] args) {
        MemoryManagementUnit mmu = MemoryManagementUnit.build();
        Cpu cpu = new Cpu(mmu);
        Gpu gpu = new Gpu(mmu);

        GambleUi gb = new GambleUi(gpu.getScreenBuffer());
        SwingUtilities.invokeLater(gb::init);
    }
}
