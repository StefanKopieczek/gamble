package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.*;

public class StandardUi extends Ui {
    private final Color[][] screenBuffer;
    private final Io io;

    public StandardUi(Color[][] screenBuffer, Io io) {
        super("Gamble");
        this.screenBuffer = screenBuffer;
        this.io = io;
    }

    @Override
    protected void setupUi() {
        Screen screen = new Screen(screenBuffer);
        screen.init();
        getContentPane().add(screen, BorderLayout.CENTER);

        Controls controls = new Controls(io);
        controls.init();
        getContentPane().add(controls, BorderLayout.SOUTH);

    }
}
