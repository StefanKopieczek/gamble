package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.event.*;

public class IoMappedButton extends JButton {
    private Io io;
    private Io.Button buttonType;
    private int keyCode;

    public IoMappedButton(String label, Io io, Io.Button buttonType, int keyCode)  {
        super(label);
        this.io = io;
        this.buttonType = buttonType;
        this.keyCode = keyCode;
    }

    public void addListeners(JComponent parent) {
        addMouseListener(buildMouseListener(io, buttonType));
        addKeyboardListener(parent, keyCode);
    }

    private void onPressed() {
        io.setButtonPressed(buttonType, true);
    }

    private void onReleased() {
        io.setButtonPressed(buttonType, false);
    }

    private MouseListener buildMouseListener(Io io, Io.Button buttonType) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                onPressed();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                onReleased();
            }
        };
    }

    private void addKeyboardListener(JComponent parent, int keyCode) {
        parent.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                // Ignore
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == keyCode) {
                    onPressed();
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == keyCode) {
                    onReleased();
                }
            }
        });
    }
}
