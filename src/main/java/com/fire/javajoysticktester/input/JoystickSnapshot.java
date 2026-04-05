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
        List<String> allDetectedControllerNames
) {
    public static JoystickSnapshot disconnected(List<String> allDetectedControllerNames) {
        return new JoystickSnapshot(false, "None", false, Collections.emptyMap(), Collections.emptyMap(), allDetectedControllerNames);
    }
}
