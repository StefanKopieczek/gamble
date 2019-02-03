package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class DPad extends JPanel {
    private final ArrowButton up;
    private final ArrowButton down;
    private final ArrowButton left;
    private final ArrowButton right;

    public DPad(Io io) {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        setBackground(Color.BLACK);

        up = new ArrowButton("▲", io, Io.Button.UP, KeyEvent.VK_UP);
        c.gridx = 1;
        c.gridy = 0;
        add(up, c);

        down = new ArrowButton("▼", io, Io.Button.DOWN, KeyEvent.VK_DOWN);
        c.gridx = 1;
        c.gridy = 2;
        add(down, c);

        left = new ArrowButton("◀", io, Io.Button.LEFT, KeyEvent.VK_LEFT);
        c.gridx = 0;
        c.gridy = 1;
        add(left, c);

        right = new ArrowButton("▶", io, Io.Button.RIGHT, KeyEvent.VK_RIGHT);
        c.gridx = 2;
        c.gridy = 1;
        add(right, c);
    }

    private static class ArrowButton extends IoMappedButton {
        ArrowButton(String label, Io io, Io.Button buttonType, int keyCode) {
            super(label, io, buttonType, keyCode, true);
            setPreferredSize(new Dimension(10, 10));
            setOpaque(false);
        }
    }
}
