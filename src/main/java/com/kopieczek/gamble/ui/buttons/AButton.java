package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class AButton extends RoundButton {
    public AButton(Io io) {
        super("A", io, Io.Button.A, KeyEvent.VK_Z);
    }
}
