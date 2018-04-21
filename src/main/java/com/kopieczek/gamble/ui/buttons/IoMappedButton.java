package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class IoMappedButton extends JButton {
    public IoMappedButton(String label, Io io, Io.Button buttonType)  {
        super(label);
        addMouseListener(buildMouseListener(io, buttonType));
    }

    private MouseListener buildMouseListener(Io io, Io.Button buttonType) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                io.setButtonPressed(buttonType, true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                io.setButtonPressed(buttonType, false);
            }
        };
    }
}
