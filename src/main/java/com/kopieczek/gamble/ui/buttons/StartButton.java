package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class StartButton extends LozengeButton {
    public StartButton(Io io) {
        super("Start", io, Io.Button.START, KeyEvent.VK_ENTER);
    }
}
