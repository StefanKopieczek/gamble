package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;

class RoundButton extends IoMappedButton {
    RoundButton(String label, Io io, Io.Button buttonType) {
        super(label, io, buttonType);
    }
}
