package com.kopieczek.gamble;

import com.kopieczek.gamble.hardware.cpu.Cpu;
import com.kopieczek.gamble.hardware.governor.Governor;
import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.cartridge.Cartridge;
import com.kopieczek.gamble.hardware.memory.cartridge.GameCartridge;
import com.kopieczek.gamble.ui.GambleUi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Gamble {
    private static final Logger log = LogManager.getLogger(Gamble.class);

    public static void main(String[] args) {
        log.info("Gamble is starting up");

        log.info("Setting up hardware");
        Mmu mmu = Mmu.build();
        Cpu cpu = new Cpu(mmu.getShieldedMemoryAccess(), mmu.getInterruptLine());
        Gpu gpu = new Gpu(mmu.getDirectMemoryAccess(), mmu.getIo(), mmu.getInterruptLine(),
                          mmu.getGraphicsAccessController());

        log.info("Loading ROM");
        loadRom(mmu, new File(args[0]));

        log.info("Initializing UI");
        GambleUi gb = new GambleUi(gpu.getScreenBuffer(), mmu.getIo());
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
            mmu.stepAhead(cycleDelta);
            gpu.stepAhead(cycleDelta);
            governor.sleepIfNeeded(cycleDelta);
        }
    }

    private static void loadRom(Mmu mmu, File file) {
        try {
            Cartridge cartridge = new GameCartridge(file);
            mmu.loadCartridge(cartridge);
        } catch (IOException e) {
            log.error("Failed to load rom", e);
        }
    }
}
