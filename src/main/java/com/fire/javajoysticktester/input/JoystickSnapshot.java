package com.fire.javajoysticktester.input;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record JoystickSnapshot(
        boolean connected,
        String controllerName,
        boolean thrustmasterT16000MDetected,
        Map<String, Float> axes,
        Map<String, Boolean> buttons,
        List<String> allDetectedControllerNames,
        String accessStatus,
        boolean linuxPermissionDenied
) {
    public static JoystickSnapshot disconnected(List<String> allDetectedControllerNames) {
        return disconnected(allDetectedControllerNames, "No joystick detected", false);
    }

    public static JoystickSnapshot disconnected(List<String> allDetectedControllerNames,
                                                String accessStatus,
                                                boolean linuxPermissionDenied) {
        return new JoystickSnapshot(
                false,
                "None",
                false,
                Collections.emptyMap(),
                Collections.emptyMap(),
                allDetectedControllerNames,
                accessStatus,
                linuxPermissionDenied
        );
    }
}
