package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.graphics.ScreenBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Screen extends JPanel implements ActionListener {
    private static final float REDRAW_HZ = 20;
    private static final int REDRAW_DELAY = (int)(1000 / REDRAW_HZ);
    private Timer repaintTimer = new Timer(REDRAW_DELAY, this);
    private final float DEFAULT_SCREEN_SCALE = 2;
    private final ScreenBuffer screenBuffer;

    Screen(ScreenBuffer screenBuffer) {
        super();
        this.screenBuffer = screenBuffer;
    }

    void init() {
        setPreferredSize(new Dimension((int)(Gpu.DISPLAY_WIDTH * DEFAULT_SCREEN_SCALE),
                                       (int)(Gpu.DISPLAY_HEIGHT * DEFAULT_SCREEN_SCALE)));
        repaintTimer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        screenBuffer.updateScreenBuffer();
        Color[][] currentFrame = screenBuffer.getScreen();
        final int width = getWidth();
        final int height = getHeight();
        final float scaleX = width/(float)Gpu.DISPLAY_WIDTH;
        final float scaleY = height/(float)Gpu.DISPLAY_HEIGHT;
        for (int y = 0; y < height ; y++) {
            for (int x = 0; x < width; x++) {
                int pixelX = (int)(x / scaleX);
                int pixelY = (int)(y / scaleY);
                setPixel(g, x, y, currentFrame[pixelY][pixelX]);
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
