package com.kopieczek.gamble.ui.buttons;

import com.kopieczek.gamble.hardware.memory.Io;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static java.awt.event.KeyEvent.KEY_PRESSED;
import static java.awt.event.KeyEvent.KEY_RELEASED;

public class IoMappedButton extends JComponent {
    private static final Color ACTIVE_COLOR = new Color(0xff, 0x60, 0x00); // Amber
    private static final Color PASSIVE_COLOR = new Color(0x8f, 0x7f, 0x00); // Golden

    private Io io;
    private Io.Button buttonType;
    private int keyCode;
    private boolean isPressed = false;
    private final boolean isSquare;

    public IoMappedButton(String label, Io io, Io.Button buttonType, int keyCode, boolean isSquare)  {
        this.io = io;
        this.buttonType = buttonType;
        this.keyCode = keyCode;
        this.isSquare = isSquare;
        this.addListeners();
    }

    @Override
    public void paintComponent(Graphics g) {
        Rectangle bounds = g.getClipBounds();
        Color activeColor = isPressed ? ACTIVE_COLOR : PASSIVE_COLOR;
        g.drawImage(createButton(bounds.width, bounds.height, activeColor, isSquare), 0, 0, this);
    }

    private void addListeners() {
        addMouseListener(buildMouseListener(io, buttonType));
        addKeyboardListener(keyCode);
    }

    private void onPressed() {
        io.setButtonPressed(buttonType, true);
        isPressed = true;
        repaint();
    }

    private void onReleased() {
        io.setButtonPressed(buttonType, false);
        isPressed = false;
        repaint();
    }

    private MouseListener buildMouseListener(Io io, Io.Button buttonType) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                onPressed();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                onReleased();
            }
        };
    }

    private void addKeyboardListener(final int keyCode) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(new KeyEventDispatcher() {
                    @Override
                    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
                        if (keyEvent.getKeyCode() != keyCode) {
                            return false;
                        }

                        if (keyEvent.getID() == KEY_PRESSED)  {
                            onPressed();
                            return true;
                        } else if (keyEvent.getID() == KEY_RELEASED) {
                            onReleased();
                            return true;
                        }

                        return false;
                    }
                });
    }

    private static BufferedImage createButton(int width, int height, Color centreColor, boolean isSquare) {
        int areaWidth = width;
        int areaHeight = height;
        int ovalWidth = width;
        int ovalHeight = height;
        BufferedImage img = new BufferedImage(areaWidth, areaHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Color[] colors = { centreColor, Color.BLACK };
        float[] dist = { 0.0f, 0.5f };
        int radius = Math.min(ovalHeight, ovalWidth);
        Point2D center = new Point2D.Float(0.5f * areaWidth, 0.5f * areaHeight);
        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
        g.setPaint(p);
        if (isSquare) {
            ovalWidth = radius;
            ovalHeight = radius;
        }
        g.fillOval(0, 0, ovalWidth, ovalHeight);
        g.dispose();
        return img;
    }
}
