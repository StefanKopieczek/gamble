package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.*;

public class DPad extends JPanel {
    public DPad(Io io) {
        super(new BorderLayout());
        add(new ArrowButton("▲"), BorderLayout.NORTH);
        add(new ArrowButton("▼"), BorderLayout.SOUTH);
        add(new ArrowButton("◀"), BorderLayout.WEST);
        add(new ArrowButton("▶"), BorderLayout.EAST);
    }

    private static class ArrowButton extends JButton {
        ArrowButton(String label) {
            super(label);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }
    }
}
