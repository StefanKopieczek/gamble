package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.Gamble;
import com.kopieczek.gamble.hardware.graphics.ScreenBuffer;
import com.kopieczek.gamble.hardware.memory.Io;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GambleUi extends JFrame {
    private static final Logger log = LogManager.getLogger(Gamble.class);

    private final ScreenBuffer screenBuffer;
    private final Io io;

    public GambleUi(ScreenBuffer screenBuffer, Io io) {
        super("Gamble");
        this.screenBuffer = screenBuffer;
        this.io = io;
    }

    public void init() {
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setDefaultLookAndFeelDecorated(true);
        getContentPane().setLayout(new BorderLayout());

        fixMacInput();

        Screen screen = new Screen(screenBuffer);
        screen.init();
        getContentPane().add(screen, BorderLayout.CENTER);

        Controls controls = new Controls(io);
        getContentPane().add(controls, BorderLayout.SOUTH);
        controls.init();

        pack();
        setVisible(true);
    }

    private void fixMacInput() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            try {
                boolean isEnabled = getMacPressAndHoldEnabled();
                if (isEnabled) {
                    setMacPressAndHold(false);
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            setMacPressAndHold(true);
                        } catch (Exception e) {
                            log.error("Failed to re-enable Mac Press-and-Hold", e);
                        }
                    }));

                }
            } catch (Exception e) {
                log.error("Failed to fix Mac repeated character input", e);
            }
        }
    }

    private boolean getMacPressAndHoldEnabled() throws Exception {
        Process p = Runtime.getRuntime().exec(new String[] {
                "defaults",
                "read",
                "NSGlobalDomain",
                "ApplePressAndHoldEnabled",
                "-bool",
        });
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String result = stdIn.readLine().toLowerCase().trim();
        return result.equals("1");
    }

    private void setMacPressAndHold(boolean enabled) throws Exception {
        Process p = Runtime.getRuntime().exec(new String[] {
                "defaults",
                "write",
                "NSGlobalDomain",
                "ApplePressAndHoldEnabled",
                "-bool",
                Boolean.toString(enabled)
        });
        p.waitFor();
    }
}
