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
    private String manualControllerSelection;

    public JoystickSnapshot poll() {
        List<Controller> usableControllers = new ArrayList<>();
        List<String> names = new ArrayList<>();
        LinuxPermissionStatus linuxPermissionStatus = probeLinuxPermissionStatus();

        final Controller[] controllers;
        try {
            controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        } catch (RuntimeException | LinkageError ex) {
            boolean permissionDenied = linuxPermissionStatus.permissionDenied() || isPermissionDenied(ex);
            boolean nativeLibraryMissing = isNativeLibraryMissing(ex);

            String status;
            if (permissionDenied) {
                status = "Linux joystick access denied (Permission denied on /dev/input/event*).";
            } else if (nativeLibraryMissing) {
                status = "JInput native library not available (jinput-linux64 missing from java.library.path).";
            } else {
                status = "Failed to query joystick controllers: " + ex.getClass().getSimpleName();
            }

            return JoystickSnapshot.disconnected(names, status, permissionDenied);
        }

        for (Controller controller : controllers) {
            if (isControllerUsable(controller)) {
                names.add(controller.getName());
                usableControllers.add(controller);
            }
        }

        Controller chosen = chooseController(usableControllers);
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
        boolean isT16000 = isT16000Name(name);

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

    public void setManualControllerSelection(String manualControllerSelection) {
        this.manualControllerSelection = (manualControllerSelection == null || manualControllerSelection.isBlank())
                ? null
                : manualControllerSelection;
    }

    public String getManualControllerSelection() {
        return manualControllerSelection;
    }

    private Controller chooseController(List<Controller> usableControllers) {
        if (usableControllers.isEmpty()) {
            return null;
        }

        if (manualControllerSelection != null) {
            for (Controller controller : usableControllers) {
                if (controller.getName().equals(manualControllerSelection)) {
                    return controller;
                }
            }
        }

        Controller best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Controller controller : usableControllers) {
            int score = scoreForAutoSelection(controller);
            if (score > bestScore) {
                bestScore = score;
                best = controller;
            }
        }

        return best;
    }

    private static int scoreForAutoSelection(Controller controller) {
        String normalizedName = controller.getName().toLowerCase(Locale.ROOT);
        int score = 0;

        if (isT16000Name(normalizedName)) {
            score += 10_000;
        }

        Controller.Type type = controller.getType();
        if (type == Controller.Type.STICK) {
            score += 500;
        } else if (type == Controller.Type.WHEEL) {
            score += 200;
        } else if (type == Controller.Type.GAMEPAD) {
            score += 100;
        }

        if (normalizedName.contains("steam") || normalizedName.contains("virtual")) {
            score -= 2_000;
        }
        if (normalizedName.contains("xbox") || normalizedName.contains("dualshock") || normalizedName.contains("wireless controller")) {
            score -= 300;
        }

        return score;
    }

    private static boolean isT16000Name(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("t.16000") || normalized.contains("t16000");
    }

    private static boolean isControllerUsable(Controller controller) {
        Controller.Type type = controller.getType();
        return type == Controller.Type.STICK || type == Controller.Type.GAMEPAD || type == Controller.Type.WHEEL;
    }

    private static boolean isPermissionDenied(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("permission denied");
    }


    private static boolean isNativeLibraryMissing(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof UnsatisfiedLinkError) {
                String message = current.getMessage();
                return message != null
                        && message.toLowerCase(Locale.ROOT).contains("jinput-linux")
                        && message.toLowerCase(Locale.ROOT).contains("java.library.path");
            }
            current = current.getCause();
        }

        return false;
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
