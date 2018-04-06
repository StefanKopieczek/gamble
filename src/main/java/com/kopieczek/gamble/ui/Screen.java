package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.graphics.Gpu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Screen extends JPanel implements ActionListener {
    private Timer repaintTimer = new Timer(17, this);
    private final Color[][] screenBuffer;

    public Screen(Color[][] screenBuffer) {
        this.screenBuffer = screenBuffer;
    }

    public void init() {
        setMinimumSize(new Dimension(Gpu.DISPLAY_WIDTH, Gpu.DISPLAY_HEIGHT));
        setPreferredSize(new Dimension(Gpu.DISPLAY_WIDTH, Gpu.DISPLAY_HEIGHT));
        setMaximumSize(new Dimension(Gpu.DISPLAY_WIDTH, Gpu.DISPLAY_HEIGHT));
        repaintTimer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        synchronized (screenBuffer) {
            for (int y = 0; y < Gpu.DISPLAY_HEIGHT; y++) {
                for (int x = 0; x < Gpu.DISPLAY_WIDTH; x++) {
                    setPixel(g, x, y, screenBuffer[y][x]);
                }
            }
        }
    }

    private void setPixel(Graphics g, int x, int y, Color c) {
        g.setColor(c);
        g.drawLine(x, y, x, y);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }
}
