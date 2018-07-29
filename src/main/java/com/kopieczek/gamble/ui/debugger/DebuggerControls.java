package com.kopieczek.gamble.ui.debugger;

import com.kopieczek.gamble.execution.ExecutionController;

import javax.swing.*;

public class DebuggerControls extends JPanel {
    private final ExecutionController controller;

    public DebuggerControls(ExecutionController controller) {
        super();
        this.controller = controller;
    }

    public void init() {
        add(new JLabel("<Debug controls go here>"));
    }
}
