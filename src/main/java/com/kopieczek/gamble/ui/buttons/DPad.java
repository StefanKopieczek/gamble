package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.*;

public class DPad extends JPanel {
    public DPad(Io io) {
        super(new BorderLayout());
        add(new ArrowButton("▲", io, Io.Button.UP), BorderLayout.NORTH);
        add(new ArrowButton("▼", io, Io.Button.DOWN), BorderLayout.SOUTH);
        add(new ArrowButton("◀", io, Io.Button.LEFT), BorderLayout.WEST);
        add(new ArrowButton("▶", io, Io.Button.RIGHT), BorderLayout.EAST);
    }

    private static class ArrowButton extends IoMappedButton {
        ArrowButton(String label, Io io, Io.Button buttonType) {
            super(label, io, buttonType);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }
    }
}
