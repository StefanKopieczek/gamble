package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.*;

class RoundButton extends IoMappedButton {
    RoundButton(String label, Io io, Io.Button buttonType, int keyCode) {
        super(label, io, buttonType, keyCode, false);
        setPreferredSize(new Dimension(23, 8));
    }
}
