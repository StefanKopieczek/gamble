package com.kopieczek.gamble;

import com.kopieczek.gamble.ui.Gameboy;

import javax.swing.*;

public class Gamble {
    public static void main(String[] args) {
        Gameboy gb = new Gameboy();
        SwingUtilities.invokeLater(gb::init);
    }
}
