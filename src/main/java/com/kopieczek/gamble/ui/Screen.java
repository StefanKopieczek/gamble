package com.kopieczek.gamble.ui;

import com.kopieczek.gamble.hardware.graphics.Gpu;
import com.kopieczek.gamble.hardware.graphics.ScreenBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class Screen extends JPanel implements ActionListener {
    private static final float REDRAW_HZ = 60;
    private static final int REDRAW_DELAY = (int)(1000 / REDRAW_HZ);
    private Timer repaintTimer = new Timer(REDRAW_DELAY, this);
    private final float DEFAULT_SCREEN_SCALE = 2;
    private final ScreenBuffer screenBuffer;
    private BufferedImage toBlit;

    Screen(ScreenBuffer screenBuffer) {
        super();
        this.screenBuffer = screenBuffer;
    }

    void init() {
        int height = (int)(Gpu.DISPLAY_HEIGHT * DEFAULT_SCREEN_SCALE);
        int width = (int)(Gpu.DISPLAY_WIDTH * DEFAULT_SCREEN_SCALE);
        setPreferredSize(new Dimension(width, height));
        toBlit = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        repaintTimer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        screenBuffer.updateScreenBuffer();
        Color[][] currentFrame = screenBuffer.getScreen();
        final int width = getWidth();
        final int height = getHeight();
        if (toBlit.getWidth() != width || toBlit.getHeight() != height) {
            toBlit = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }

        final float scaleX = width/(float)Gpu.DISPLAY_WIDTH;
        final float scaleY = height/(float)Gpu.DISPLAY_HEIGHT;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelX = (int)(x / scaleX);
                int pixelY = (int)(y / scaleY);
                toBlit.setRGB(x, y, currentFrame[pixelY][pixelX].getRGB());
            }
        }

        g.drawImage(toBlit, 0, 0, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }
}
