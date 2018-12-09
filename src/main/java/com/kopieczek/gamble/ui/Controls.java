package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.hardware.memory.Io;
import com.kopieczek.gamble.ui.buttons.*;

import javax.swing.*;

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
        setFocusable(true);
        requestFocusInWindow();
        add(dPad);
        add(a);
        add(b);
        add(start);
        add(select);
        dPad.addListeners(this);
        a.addListeners(this);
        b.addListeners(this);
        start.addListeners(this);
        select.addListeners(this);
    }
}
