package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.hardware.memory.Io;
import com.kopieczek.gamble.ui.buttons.*;

import javax.swing.*;
import java.awt.*;

class Controls extends JPanel {
    private final DPad dPad;
    private final IoMappedButton a;
    private final IoMappedButton b;
    private final IoMappedButton start;
    private final IoMappedButton select;

    Controls(Io io) {
        super();
        dPad = new DPad(io);
        a = new AButton(io);
        b = new BButton(io);
        start = new StartButton(io);
        select = new SelectButton(io);
    }

    void init() {
        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();
        add(dPad);
        add(a);
        add(b);
        add(start);
        add(select);
    }
}
