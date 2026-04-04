package com.fire.javajoysticktester.input;

import com.fire.javajoysticktester.model.ShipState;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Keyboard fallback input provider.
 *
 * Later, this can be replaced or supplemented by a joystick implementation
 * that applies the same style of updates to {@link ShipState}.
 */
public class KeyboardInput implements KeyListener {
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean qPressed;
    private boolean ePressed;
    private boolean wPressed;
    private boolean sPressed;

    private final double angularSpeedDegPerSec = 120.0;
    private final double throttleUnitsPerSec = 0.55;

    private final boolean autoCenterPitch = true;
    private final boolean autoCenterYaw = true;
    private final boolean autoCenterRoll = true;
    private final double autoCenterDegPerSec = 180.0;

    /**
     * Apply currently active keys to the ship model once per update tick.
     *
     * @param shipState mutable ship state
     * @param deltaTimeSec elapsed time in seconds since last update
     */
    public void update(ShipState shipState, double deltaTimeSec) {
        double angleStep = angularSpeedDegPerSec * deltaTimeSec;
        double throttleStep = throttleUnitsPerSec * deltaTimeSec;
        double centerStep = autoCenterDegPerSec * deltaTimeSec;

        if (upPressed) {
            shipState.addPitchTarget(-angleStep);
        }
        if (downPressed) {
            shipState.addPitchTarget(angleStep);
        }
        if (!upPressed && !downPressed && autoCenterPitch) {
            shipState.nudgePitchTargetTowardCenter(centerStep);
        }

        if (leftPressed) {
            shipState.addYawTarget(-angleStep);
        }
        if (rightPressed) {
            shipState.addYawTarget(angleStep);
        }
        if (!leftPressed && !rightPressed && autoCenterYaw) {
            shipState.nudgeYawTargetTowardCenter(centerStep);
        }

        if (qPressed) {
            shipState.addRollTarget(-angleStep);
        }
        if (ePressed) {
            shipState.addRollTarget(angleStep);
        }
        if (!qPressed && !ePressed && autoCenterRoll) {
            shipState.nudgeRollTargetTowardCenter(centerStep);
        }

        if (wPressed) {
            shipState.addThrottleTarget(throttleStep);
        }
        if (sPressed) {
            shipState.addThrottleTarget(-throttleStep);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used.
    }

    @Override
    public void keyPressed(KeyEvent e) {
        setKeyState(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        setKeyState(e.getKeyCode(), false);
    }

    private void setKeyState(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_UP -> upPressed = pressed;
            case KeyEvent.VK_DOWN -> downPressed = pressed;
            case KeyEvent.VK_LEFT -> leftPressed = pressed;
            case KeyEvent.VK_RIGHT -> rightPressed = pressed;
            case KeyEvent.VK_Q -> qPressed = pressed;
            case KeyEvent.VK_E -> ePressed = pressed;
            case KeyEvent.VK_W -> wPressed = pressed;
            case KeyEvent.VK_S -> sPressed = pressed;
            default -> {
                // Ignore unsupported keys.
            }
        }
    }
}
