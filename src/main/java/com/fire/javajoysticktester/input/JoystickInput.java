package com.fire.javajoysticktester.input;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Joystick polling wrapper using JInput.
 */
public class JoystickInput {
    private Controller activeController;

    public JoystickSnapshot poll() {
        List<String> names = new ArrayList<>();
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

        Controller chosen = null;
        for (Controller controller : controllers) {
            if (isControllerUsable(controller)) {
                names.add(controller.getName());
                if (chosen == null) {
                    chosen = controller;
                }
            }
        }

        activeController = chosen;
        if (activeController == null) {
            return JoystickSnapshot.disconnected(names);
        }

        if (!activeController.poll()) {
            return JoystickSnapshot.disconnected(names);
        }

        Map<String, Float> axes = new LinkedHashMap<>();
        Map<String, Boolean> buttons = new LinkedHashMap<>();

        for (Component component : activeController.getComponents()) {
            float value = component.getPollData();
            String name = component.getName();
            if (component.isAnalog()) {
                axes.put(name, value);
            } else {
                buttons.put(name, value > 0.5f);
            }
        }

        String name = activeController.getName();
        boolean isT16000 = name.toLowerCase(Locale.ROOT).contains("t.16000")
                || name.toLowerCase(Locale.ROOT).contains("t16000");

        return new JoystickSnapshot(true, name, isT16000, axes, buttons, names);
    }

    private static boolean isControllerUsable(Controller controller) {
        Controller.Type type = controller.getType();
        return type == Controller.Type.STICK || type == Controller.Type.GAMEPAD || type == Controller.Type.WHEEL;
    }
}
