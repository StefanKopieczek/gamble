package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.hardware.memory.Io;
import com.kopieczek.gamble.ui.buttons.*;

import javax.swing.*;

class Controls extends JPanel {
    private final JComponent dPad;
    private final JComponent a;
    private final JComponent b;
    private final JComponent start;
    private final JComponent select;

    Controls(Io io) {
        super();
        dPad = new DPad(io);
        a = new AButton(io);
        b = new BButton(io);
        start = new StartButton(io);
        select = new SelectButton(io);
    }

    void init() {
        add(dPad);
        add(a);
        add(b);
        add(start);
        add(select);
    }
}
