package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class BButton extends RoundButton {
    public BButton(Io io) {
        super("B", io, Io.Button.B, KeyEvent.VK_B);
    }
}
