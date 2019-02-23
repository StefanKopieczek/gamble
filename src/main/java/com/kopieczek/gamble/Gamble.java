package com.kopieczek.gamble;

import com.kopieczek.gamble.hardware.audio.Apu;
import com.kopieczek.gamble.hardware.audio.StereoRenderer;
import com.kopieczek.gamble.hardware.cpu.Cpu;
import com.kopieczek.gamble.hardware.cpu.Word;
import com.kopieczek.gamble.hardware.cpu.timer.TimerChip;
import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.memory.Mmu;
import com.kopieczek.gamble.hardware.memory.cartridge.Cartridge;
import com.kopieczek.gamble.hardware.memory.cartridge.CartridgeLoader;
import com.kopieczek.gamble.savefiles.HashMapDb;
import com.kopieczek.gamble.savefiles.SaveFileDb;
import com.kopieczek.gamble.ui.GambleUi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Gamble {
    private static final Logger log = LogManager.getLogger(Gamble.class);
    private static final boolean SHOULD_SKIP_BIOS = false;

    public static void main(String[] args) {
        log.info("Gamble is starting up");

        log.info("Setting up hardware");
        Mmu mmu = Mmu.build(SHOULD_SKIP_BIOS);
        Cpu cpu = new Cpu(mmu.getShieldedMemoryAccess(), mmu.getInterruptLine());
        Gpu gpu = new Gpu(mmu.getDirectMemoryAccess(),
                          mmu.getIo(),
                          mmu.getInterruptLine(),
                          mmu.getGraphicsAccessController(),
                          mmu.getOam(),
                          mmu.getVram());
        TimerChip timer = new TimerChip(mmu.getIo(), mmu.getInterruptLine());
        Apu apu = new Apu(mmu.getIo(), getRenderer());

        log.info("Loading ROM");
        SaveFileDb<String> saveFileDb = HashMapDb.initialize(new File(System.getProperty("user.home"), ".gambledb"));
        loadRom(mmu, saveFileDb, new File(args[0]));

        log.info("Initializing UI");
        GambleUi gb = new GambleUi(gpu.getScreenBuffer(), mmu.getIo());
        SwingUtilities.invokeLater(gb::init);

        if (SHOULD_SKIP_BIOS) {
            mmu.setBiosEnabled(false);
            cpu.setProgramCounter(0x100);
            cpu.set(Word.Register.AF, Word.literal(0x01b0));
            cpu.set(Word.Register.BC, Word.literal(0x0013));
            cpu.set(Word.Register.DE, Word.literal(0x00d8));
            cpu.set(Word.Register.HL, Word.literal(0x014d));
            cpu.set(Word.Register.SP, Word.literal((0xfffe)));
        }

        log.info("Gamble started");
        while (true) {
            int cyclesBefore = cpu.getCycles();
            cpu.tick();
            int cycleDelta = cpu.getCycles() - cyclesBefore;
            if (cpu.isStopped()) {
                gpu.stop();
            } else {
                mmu.stepAhead(cycleDelta);
                gpu.stepAhead(cycleDelta);
            }
            apu.stepAhead(cycleDelta);
            timer.tick(cycleDelta);
        }
    }

    private static void loadRom(Mmu mmu, SaveFileDb<String> saveFileDb, File file) {
        try {
            Cartridge cartridge = CartridgeLoader.loadFrom(file);
            cartridge.setSaveFileDb(saveFileDb);
            if (cartridge.hasSaveData()) {
                cartridge.loadFromSave();
            }
            mmu.loadCartridge(cartridge);
        } catch (IOException e) {
            log.error("Failed to load rom", e);
        }
    }

    private static StereoRenderer getRenderer() {
        StereoRenderer renderer = new StereoRenderer();
        try {
            renderer.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return renderer;
    }
}
