package com.kopieczek.gamble.ui;

import javax.swing.*;
import java.awt.*;

public class GambleUi extends JFrame {
    private final Color[][] screenBuffer;

    public GambleUi(Color[][] screenBuffer) {
        super("Gamble Gameboy Emulator");
        this.screenBuffer = screenBuffer;
    }

    public void init() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setDefaultLookAndFeelDecorated(true);
        getContentPane().setLayout(new BorderLayout());

        Screen screen = new Screen(screenBuffer);
        screen.init();
        getContentPane().add(screen, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }
}
