package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.graphics.Screen;

import javax.swing.*;
import java.awt.*;

public class Gameboy extends JFrame {
    public Gameboy() {
        super("Gamble Gameboy Emulator");
    }

    public void init() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setDefaultLookAndFeelDecorated(true);
        getContentPane().setLayout(new BorderLayout());

        Screen screen = new Screen();
        screen.init();
        getContentPane().add(screen, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }
}
