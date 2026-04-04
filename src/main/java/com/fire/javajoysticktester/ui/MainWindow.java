package com.fire.javajoysticktester.ui;

import com.fire.javajoysticktester.input.KeyboardInput;
import com.fire.javajoysticktester.model.ShipState;
import com.fire.javajoysticktester.render.ShipPanel;

import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.BorderLayout;

/**
 * Main application frame.
 *
 * Update/render flow:
 * 1) A Swing timer ticks at a fixed interval (target 60 FPS).
 * 2) Each tick computes delta-time and applies input updates to ShipState.
 * 3) The panel is repainted, reading ShipState to render orientation + HUD.
 */
public class MainWindow {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int TARGET_FPS = 60;

    private final JFrame frame;
    private final ShipState shipState;
    private final ShipPanel shipPanel;
    private final KeyboardInput keyboardInput;

    private long lastUpdateNanos;

    public MainWindow() {
        frame = new JFrame("Java Joystick Tester");
        shipState = new ShipState();
        shipPanel = new ShipPanel(shipState);
        keyboardInput = new KeyboardInput();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(shipPanel, BorderLayout.CENTER);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);

        frame.addKeyListener(keyboardInput);
    }

    public void showWindow() {
        frame.setVisible(true);
        shipPanel.requestFocusInWindow();
        startLoop();
    }

    private void startLoop() {
        int delayMs = 1000 / TARGET_FPS;
        lastUpdateNanos = System.nanoTime();

        Timer timer = new Timer(delayMs, event -> {
            long now = System.nanoTime();
            double deltaSec = (now - lastUpdateNanos) / 1_000_000_000.0;
            lastUpdateNanos = now;

            keyboardInput.update(shipState, deltaSec);
            shipState.update(deltaSec);
            shipPanel.repaint();
        });

        timer.setCoalesce(true);
        timer.start();
    }
}
