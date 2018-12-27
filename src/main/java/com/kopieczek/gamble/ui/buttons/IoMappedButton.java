package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static java.awt.event.KeyEvent.KEY_PRESSED;
import static java.awt.event.KeyEvent.KEY_RELEASED;
import static java.awt.event.KeyEvent.KEY_TYPED;

public class IoMappedButton extends JButton {
    private Io io;
    private Io.Button buttonType;
    private int keyCode;

    public IoMappedButton(String label, Io io, Io.Button buttonType, int keyCode)  {
        super(label);
        this.io = io;
        this.buttonType = buttonType;
        this.keyCode = keyCode;

        this.addListeners();
    }

    private void addListeners() {
        addMouseListener(buildMouseListener(io, buttonType));
        addKeyboardListener(keyCode);
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

    private void addKeyboardListener(final int keyCode) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(new KeyEventDispatcher() {
                    @Override
                    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
                        if (keyEvent.getKeyCode() != keyCode) {
                            return false;
                        }

                        if (keyEvent.getID() == KEY_PRESSED)  {
                            onPressed();
                            return true;
                        } else if (keyEvent.getID() == KEY_RELEASED) {
                            onReleased();
                            return true;
                        }

                        return false;
                    }
                });
    }
}
