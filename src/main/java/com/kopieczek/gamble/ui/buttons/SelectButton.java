package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class SelectButton extends LozengeButton {
    public SelectButton(Io io) {
        super("Select", io, Io.Button.SELECT, KeyEvent.VK_BACK_SPACE);
    }
}
