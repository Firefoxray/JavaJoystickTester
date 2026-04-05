package com.fire.javajoysticktester.input;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keyboard input handler implemented with Swing key bindings.
 *
 * Key bindings are attached using WHEN_IN_FOCUSED_WINDOW, which is more reliable
 * than KeyListener for game-style controls in Swing.
 */
public class KeyboardInput {
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean qPressed;
    private boolean ePressed;
    private boolean wPressed;
    private boolean sPressed;

    public void install(JComponent component) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        bind(component, inputMap, KeyEvent.VK_UP, "pitchUp", () -> upPressed = true, () -> upPressed = false);
        bind(component, inputMap, KeyEvent.VK_DOWN, "pitchDown", () -> downPressed = true, () -> downPressed = false);
        bind(component, inputMap, KeyEvent.VK_LEFT, "yawLeft", () -> leftPressed = true, () -> leftPressed = false);
        bind(component, inputMap, KeyEvent.VK_RIGHT, "yawRight", () -> rightPressed = true, () -> rightPressed = false);
        bind(component, inputMap, KeyEvent.VK_Q, "rollLeft", () -> qPressed = true, () -> qPressed = false);
        bind(component, inputMap, KeyEvent.VK_E, "rollRight", () -> ePressed = true, () -> ePressed = false);
        bind(component, inputMap, KeyEvent.VK_W, "throttleUp", () -> wPressed = true, () -> wPressed = false);
        bind(component, inputMap, KeyEvent.VK_S, "throttleDown", () -> sPressed = true, () -> sPressed = false);
    }

    public KeyboardSnapshot snapshot() {
        return new KeyboardSnapshot(upPressed, downPressed, leftPressed, rightPressed, qPressed, ePressed, wPressed, sPressed);
    }

    public boolean hasAnyInputActive() {
        return upPressed || downPressed || leftPressed || rightPressed || qPressed || ePressed || wPressed || sPressed;
    }

    public static Map<String, String> getBindingDescriptions() {
        Map<String, String> bindings = new LinkedHashMap<>();
        bindings.put("Pitch Up", "Up Arrow");
        bindings.put("Pitch Down", "Down Arrow");
        bindings.put("Yaw Left", "Left Arrow");
        bindings.put("Yaw Right", "Right Arrow");
        bindings.put("Roll Left", "Q");
        bindings.put("Roll Right", "E");
        bindings.put("Throttle Up", "W");
        bindings.put("Throttle Down", "S");
        return bindings;
    }

    private static void bind(
            JComponent component,
            InputMap inputMap,
            int keyCode,
            String actionKey,
            Runnable onPress,
            Runnable onRelease
    ) {
        String pressedKey = actionKey + ".pressed";
        String releasedKey = actionKey + ".released";

        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, false), pressedKey);
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, true), releasedKey);

        component.getActionMap().put(pressedKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPress.run();
            }
        });

        component.getActionMap().put(releasedKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRelease.run();
            }
        });
    }

    public record KeyboardSnapshot(
            boolean up,
            boolean down,
            boolean left,
            boolean right,
            boolean rollLeft,
            boolean rollRight,
            boolean throttleUp,
            boolean throttleDown
    ) {
    }
}
