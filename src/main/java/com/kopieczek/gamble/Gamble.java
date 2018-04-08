package com.kopieczek.gamble;

import com.kopieczek.gamble.hardware.cpu.Cpu;
import com.kopieczek.gamble.hardware.governor.Governor;
import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.ui.GambleUi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class Gamble {
    private static final Logger log = LogManager.getLogger(Gamble.class);

    public static void main(String[] args) {
        log.info("Gamble is starting up");

        log.info("Setting up hardware");
        Mmu mmu = Mmu.build();
        setBios(mmu);
        Cpu cpu = new Cpu(mmu);
        Gpu gpu = new Gpu(mmu);

        log.info("Initializing UI");
        GambleUi gb = new GambleUi(gpu.getScreenBuffer());
        SwingUtilities.invokeLater(gb::init);
        Governor governor = new Governor();

        log.info("UI ready. Waiting 3s before starting program execution");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Gamble started");
        while (true) {
            int cyclesBefore = cpu.getCycles();
            cpu.tick();
            int cycleDelta = cpu.getCycles() - cyclesBefore;
            gpu.stepAhead(cycleDelta);
            governor.sleepIfNeeded(cycleDelta);
        }
    }

    private static void setBios(Mmu mmu) {
        final int[] cartridgeHeader = {
                0xce, 0xed, 0x66, 0x66, 0xcc, 0x0d, 0x00, 0x0b, 0x03, 0x73, 0x00, 0x83, 0x00, 0x0c, 0x00, 0x0d,
                0x00, 0x08, 0x11, 0x1f, 0x88, 0x89, 0x00, 0x0e, 0xdc, 0xcc, 0x6e, 0xe6, 0xdd, 0xdd, 0xd9, 0x99,
                0xbb, 0xbb, 0x67, 0x63, 0x6e, 0x0e, 0xec, 0xcc, 0xdd, 0xdc, 0x99, 0x9f, 0xbb, 0xb9, 0x33, 0x3e
        };
        for (int idx = 0x0104; idx < cartridgeHeader.length + 0x0104; idx ++) {
            mmu.setByte(idx, cartridgeHeader[idx - 0x0104]);
        }
    }
}
