package com.kopieczek.gamble;

import com.kopieczek.gamble.execution.ExecutionController;
import com.kopieczek.gamble.hardware.cpu.Cpu;
import com.kopieczek.gamble.hardware.governor.Governor;
import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.cartridge.Cartridge;
import com.kopieczek.gamble.hardware.memory.cartridge.CartridgeLoader;
import com.kopieczek.gamble.ui.DebuggerUi;
import com.kopieczek.gamble.ui.StandardUi;
import com.kopieczek.gamble.ui.Ui;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Gamble {
    private static final Logger log = LogManager.getLogger(Gamble.class);
    private static final boolean SHOULD_SKIP_BIOS = false;
    private static final boolean USE_DEBUGGER = true;

    public static void main(String[] args) {
        log.info("Gamble is starting up");

        log.info("Setting up hardware");
        Mmu mmu = Mmu.build(SHOULD_SKIP_BIOS);
        Cpu cpu = new Cpu(mmu.getShieldedMemoryAccess(), mmu.getInterruptLine());
        Gpu gpu = new Gpu(mmu.getDirectMemoryAccess(), mmu.getIo(), mmu.getInterruptLine(),
                          mmu.getGraphicsAccessController());

        log.info("Loading ROM");
        loadRom(mmu, new File(args[0]));

        log.info("Initializing UI");
        ExecutionController controller = new ExecutionController(cpu, mmu, gpu);
        Ui ui = buildUi(cpu, gpu, mmu, controller);
        SwingUtilities.invokeLater(ui::init);

        log.info("UI ready. Waiting 3s before starting program execution");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Gamble started");
        controller.runBlocking();
    }

    private static Ui buildUi(Cpu cpu, Gpu gpu, Mmu mmu, ExecutionController controller) {
        if (USE_DEBUGGER) {
            return new DebuggerUi(cpu, mmu, gpu, controller);
        } else {
            return new StandardUi(gpu.getScreenBuffer(), mmu.getIo());
        }
    }

    private static void loadRom(Mmu mmu, File file) {
        try {
            Cartridge cartridge = CartridgeLoader.loadFrom(file);
            mmu.loadCartridge(cartridge);
        } catch (IOException e) {
            log.error("Failed to load rom", e);
        }
    }
}
