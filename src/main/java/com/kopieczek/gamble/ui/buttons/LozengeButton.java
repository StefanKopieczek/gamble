package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;

class LozengeButton extends IoMappedButton {
    LozengeButton(String label, Io io, Io.Button buttonType) {
        super(label, io, buttonType);
    }
}