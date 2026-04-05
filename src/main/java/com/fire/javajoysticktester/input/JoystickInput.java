package com.fire.javajoysticktester.input;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        LinuxPermissionStatus linuxPermissionStatus = probeLinuxPermissionStatus();

        final Controller[] controllers;
        try {
            controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        } catch (RuntimeException ex) {
            boolean permissionDenied = linuxPermissionStatus.permissionDenied() || isPermissionDenied(ex);
            String status = permissionDenied
                    ? "Linux joystick access denied (Permission denied on /dev/input/event*)."
                    : "Failed to query joystick controllers: " + ex.getClass().getSimpleName();
            return JoystickSnapshot.disconnected(names, status, permissionDenied);
        }

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
            String status = linuxPermissionStatus.permissionDenied()
                    ? linuxPermissionStatus.statusText()
                    : "No joystick detected";
            return JoystickSnapshot.disconnected(names, status, linuxPermissionStatus.permissionDenied());
        }

        if (!activeController.poll()) {
            String status = linuxPermissionStatus.permissionDenied()
                    ? linuxPermissionStatus.statusText()
                    : "Joystick detected but poll failed";
            return JoystickSnapshot.disconnected(names, status, linuxPermissionStatus.permissionDenied());
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

        return new JoystickSnapshot(
                true,
                name,
                isT16000,
                axes,
                buttons,
                names,
                linuxPermissionStatus.statusText(),
                linuxPermissionStatus.permissionDenied()
        );
    }

    private static boolean isControllerUsable(Controller controller) {
        Controller.Type type = controller.getType();
        return type == Controller.Type.STICK || type == Controller.Type.GAMEPAD || type == Controller.Type.WHEEL;
    }

    private static boolean isPermissionDenied(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("permission denied");
    }

    private static LinuxPermissionStatus probeLinuxPermissionStatus() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!osName.contains("linux")) {
            return new LinuxPermissionStatus("Joystick access status not checked (non-Linux OS)", false);
        }

        Path inputDir = Path.of("/dev/input");
        if (!Files.isDirectory(inputDir)) {
            return new LinuxPermissionStatus("Linux joystick path /dev/input not found", false);
        }

        boolean foundAnyEventNode = false;
        boolean unreadableEventNodeFound = false;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "event*")) {
            for (Path eventNode : stream) {
                foundAnyEventNode = true;
                if (!Files.isReadable(eventNode)) {
                    unreadableEventNodeFound = true;
                    break;
                }
            }
        } catch (Exception ex) {
            return new LinuxPermissionStatus("Could not inspect /dev/input/event* permissions", false);
        }

        if (!foundAnyEventNode) {
            return new LinuxPermissionStatus("No /dev/input/event* devices found", false);
        }

        if (unreadableEventNodeFound) {
            return new LinuxPermissionStatus(
                    "Linux joystick access likely blocked: unreadable /dev/input/event* (check group/udev/logind permissions)",
                    true
            );
        }

        return new LinuxPermissionStatus("Linux joystick permission check: readable /dev/input/event*", false);
    }

    private record LinuxPermissionStatus(String statusText, boolean permissionDenied) {
    }
}
