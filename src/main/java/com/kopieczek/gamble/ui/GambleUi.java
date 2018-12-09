package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.*;

public class GambleUi extends JFrame {
    private final Color[][] screenBuffer;
    private final Io io;

    public GambleUi(Color[][] screenBuffer, Io io) {
        super("Gamble");
        this.screenBuffer = screenBuffer;
        this.io = io;
    }

    public void init() {
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setDefaultLookAndFeelDecorated(true);
        getContentPane().setLayout(new BorderLayout());

        Screen screen = new Screen(screenBuffer);
        screen.init();
        getContentPane().add(screen, BorderLayout.CENTER);

        Controls controls = new Controls(io);
        getContentPane().add(controls, BorderLayout.SOUTH);
        controls.init();

        pack();
        setVisible(true);
    }
}
