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
        super(new BorderLayout());
        up = new ArrowButton("▲", io, Io.Button.UP, KeyEvent.VK_UP);
        down = new ArrowButton("▼", io, Io.Button.DOWN, KeyEvent.VK_DOWN);
        left = new ArrowButton("◀", io, Io.Button.LEFT, KeyEvent.VK_LEFT);
        right = new ArrowButton("▶", io, Io.Button.RIGHT, KeyEvent.VK_RIGHT);
        add(up, BorderLayout.NORTH);
        add(down, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    private static class ArrowButton extends IoMappedButton {
        ArrowButton(String label, Io io, Io.Button buttonType, int keyCode) {
            super(label, io, buttonType, keyCode);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }
    }
}
