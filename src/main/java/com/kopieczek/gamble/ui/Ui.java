package com.kopieczek.gamble.ui;

import javax.swing.*;
import java.awt.*;

public abstract class Ui extends JFrame {
    Ui(String windowName) {
        super(windowName);
    }

    public void init() {
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setDefaultLookAndFeelDecorated(true);
        getContentPane().setLayout(new BorderLayout());
        setupUi();
        pack();
        setVisible(true);
    }

    protected abstract void setupUi();
}
