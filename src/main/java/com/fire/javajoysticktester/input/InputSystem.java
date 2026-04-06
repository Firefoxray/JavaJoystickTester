package com.fire.javajoysticktester.input;

import com.fire.javajoysticktester.model.ShipState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies keyboard + joystick input to ShipState through a shared flow.
 */
public class InputSystem {
    private static final double ANGULAR_SPEED_DEG_PER_SEC = 120.0;
    private static final double THROTTLE_UNITS_PER_SEC = 0.55;
    private static final double AUTO_CENTER_DEG_PER_SEC = 180.0;
    private static final double JOYSTICK_DEADZONE = 0.08;

    private static final double STARFOX_MAX_PITCH = 42.0;
    private static final double STARFOX_MAX_YAW = 55.0;
    private static final double STARFOX_MAX_ROLL = 70.0;

    private final KeyboardInput keyboardInput;
    private final JoystickInput joystickInput;

    private PreferredInputDevice preferredInputDevice = PreferredInputDevice.AUTO;
    private JoystickSnapshot lastJoystickSnapshot = JoystickSnapshot.disconnected(java.util.List.of());
    private String activeInputDescription = "Keyboard";

    private JoystickAxisOption pitchAxis = JoystickAxisOption.Y;
    private JoystickAxisOption yawAxis = JoystickAxisOption.X;
    private JoystickAxisOption rollAxis = JoystickAxisOption.RZ;
    private JoystickAxisOption throttleAxis = JoystickAxisOption.SLIDER;
    private boolean invertPitch;
    private boolean invertYaw;
    private boolean invertRoll;
    private boolean invertThrottle;
    private int triggerButtonIndex = 0;
    private int boostButtonIndex = 1;
    private JoystickButtonAction triggerButtonAction = JoystickButtonAction.FIRE_PRIMARY;
    private boolean solidPlaneEnabled = true;
    private boolean debugModeEnabled;
    private boolean firePrimaryActive;
    private boolean boostActive;

    private final Map<String, Map<Integer, String>> manualButtonMappings = new LinkedHashMap<>();
    private Runnable settingsChangedListener = () -> {
    };
    private boolean suppressSettingsChangeNotifications;

    public InputSystem(KeyboardInput keyboardInput, JoystickInput joystickInput) {
        this.keyboardInput = keyboardInput;
        this.joystickInput = joystickInput;
    }

    public void update(ShipState shipState, double deltaTimeSec) {
        lastJoystickSnapshot = joystickInput.poll();
        firePrimaryActive = false;
        boostActive = false;

        boolean joystickShouldDrive = switch (preferredInputDevice) {
            case JOYSTICK -> lastJoystickSnapshot.connected();
            case KEYBOARD -> false;
            case AUTO -> lastJoystickSnapshot.connected();
        };

        if (joystickShouldDrive) {
            applyJoystick(shipState, lastJoystickSnapshot, deltaTimeSec);
            activeInputDescription = "Joystick: " + lastJoystickSnapshot.controllerName();
        } else {
            applyKeyboard(shipState, keyboardInput.snapshot(), deltaTimeSec);
            activeInputDescription = "Keyboard";
        }
    }

    public void setSettingsChangedListener(Runnable settingsChangedListener) {
        this.settingsChangedListener = settingsChangedListener == null ? () -> {
        } : settingsChangedListener;
    }

    public void runWithoutSettingsNotifications(Runnable task) {
        boolean previous = suppressSettingsChangeNotifications;
        suppressSettingsChangeNotifications = true;
        try {
            task.run();
        } finally {
            suppressSettingsChangeNotifications = previous;
        }
    }

    public JoystickSnapshot pollJoystickSnapshotNow() {
        lastJoystickSnapshot = joystickInput.poll();
        return lastJoystickSnapshot;
    }

    public void setPreferredInputDevice(PreferredInputDevice preferredInputDevice) {
        this.preferredInputDevice = preferredInputDevice;
        onSettingsChanged();
    }

    public PreferredInputDevice getPreferredInputDevice() {
        return preferredInputDevice;
    }

    public JoystickSnapshot getLastJoystickSnapshot() {
        return lastJoystickSnapshot;
    }

    public boolean isKeyboardActive() {
        return keyboardInput.hasAnyInputActive();
    }

    public void setSelectedJoystickName(String selectedJoystickName) {
        joystickInput.setManualControllerSelection(selectedJoystickName);
        onSettingsChanged();
    }

    public String getSelectedJoystickName() {
        return joystickInput.getManualControllerSelection();
    }

    public List<String> getDetectedJoystickNames() {
        return lastJoystickSnapshot.allDetectedControllerNames();
    }

    public String getActiveInputDescription() {
        return activeInputDescription;
    }

    public boolean isSolidPlaneEnabled() {
        return solidPlaneEnabled;
    }

    public void setSolidPlaneEnabled(boolean solidPlaneEnabled) {
        this.solidPlaneEnabled = solidPlaneEnabled;
        onSettingsChanged();
    }

    public boolean isDebugModeEnabled() {
        return debugModeEnabled;
    }

    public void setDebugModeEnabled(boolean debugModeEnabled) {
        this.debugModeEnabled = debugModeEnabled;
        onSettingsChanged();
    }

    public JoystickAxisOption getPitchAxis() {
        return pitchAxis;
    }

    public void setPitchAxis(JoystickAxisOption pitchAxis) {
        this.pitchAxis = pitchAxis;
        onSettingsChanged();
    }

    public JoystickAxisOption getYawAxis() {
        return yawAxis;
    }

    public void setYawAxis(JoystickAxisOption yawAxis) {
        this.yawAxis = yawAxis;
        onSettingsChanged();
    }

    public JoystickAxisOption getRollAxis() {
        return rollAxis;
    }

    public void setRollAxis(JoystickAxisOption rollAxis) {
        this.rollAxis = rollAxis;
        onSettingsChanged();
    }

    public JoystickAxisOption getThrottleAxis() {
        return throttleAxis;
    }

    public void setThrottleAxis(JoystickAxisOption throttleAxis) {
        this.throttleAxis = throttleAxis;
        onSettingsChanged();
    }

    public boolean isInvertPitch() {
        return invertPitch;
    }

    public void setInvertPitch(boolean invertPitch) {
        this.invertPitch = invertPitch;
        onSettingsChanged();
    }

    public boolean isInvertYaw() {
        return invertYaw;
    }

    public void setInvertYaw(boolean invertYaw) {
        this.invertYaw = invertYaw;
        onSettingsChanged();
    }

    public boolean isInvertRoll() {
        return invertRoll;
    }

    public void setInvertRoll(boolean invertRoll) {
        this.invertRoll = invertRoll;
        onSettingsChanged();
    }

    public boolean isInvertThrottle() {
        return invertThrottle;
    }

    public void setInvertThrottle(boolean invertThrottle) {
        this.invertThrottle = invertThrottle;
        onSettingsChanged();
    }

    public int getTriggerButtonIndex() {
        return triggerButtonIndex;
    }

    public void setTriggerButtonIndex(int triggerButtonIndex) {
        this.triggerButtonIndex = Math.max(0, triggerButtonIndex);
        onSettingsChanged();
    }

    public int getBoostButtonIndex() {
        return boostButtonIndex;
    }

    public void setBoostButtonIndex(int boostButtonIndex) {
        this.boostButtonIndex = Math.max(0, boostButtonIndex);
        onSettingsChanged();
    }

    public JoystickButtonAction getTriggerButtonAction() {
        return triggerButtonAction;
    }

    public void setTriggerButtonAction(JoystickButtonAction triggerButtonAction) {
        this.triggerButtonAction = triggerButtonAction;
        onSettingsChanged();
    }

    public void setManualMappingForController(String controllerName, Map<Integer, String> mapping) {
        if (controllerName == null || controllerName.isBlank()) {
            return;
        }
        Map<Integer, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : mapping.entrySet()) {
            if (entry.getKey() < 0 || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            sanitized.put(entry.getKey(), entry.getValue());
        }
        if (sanitized.isEmpty()) {
            manualButtonMappings.remove(controllerName);
        } else {
            manualButtonMappings.put(controllerName, sanitized);
        }
        onSettingsChanged();
    }

    public Map<Integer, String> getManualMappingForController(String controllerName) {
        if (controllerName == null) {
            return Map.of();
        }
        Map<Integer, String> mapping = manualButtonMappings.get(controllerName);
        if (mapping == null) {
            return Map.of();
        }
        return new LinkedHashMap<>(mapping);
    }

    public Map<String, Map<Integer, String>> getManualButtonMappings() {
        Map<String, Map<Integer, String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Integer, String>> entry : manualButtonMappings.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }

    public void setManualButtonMappings(Map<String, Map<Integer, String>> mappings) {
        manualButtonMappings.clear();
        if (mappings != null) {
            for (Map.Entry<String, Map<Integer, String>> entry : mappings.entrySet()) {
                String controllerName = entry.getKey();
                if (controllerName == null || controllerName.isBlank()) {
                    continue;
                }
                Map<Integer, String> sanitized = new LinkedHashMap<>();
                for (Map.Entry<Integer, String> mapEntry : entry.getValue().entrySet()) {
                    if (mapEntry.getKey() < 0 || mapEntry.getValue() == null || mapEntry.getValue().isBlank()) {
                        continue;
                    }
                    sanitized.put(mapEntry.getKey(), mapEntry.getValue());
                }
                if (!sanitized.isEmpty()) {
                    manualButtonMappings.put(controllerName, sanitized);
                }
            }
        }
        onSettingsChanged();
    }

    public List<Integer> getLogicalButtonIndices(JoystickSnapshot snapshot) {
        List<Integer> logicalButtons = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            logicalButtons.add(i);
        }
        return logicalButtons;
    }

    public boolean isLogicalButtonPressed(JoystickSnapshot snapshot, int targetButtonIndex) {
        Map<Integer, String> mapping = getManualMappingForController(snapshot.controllerName());
        if (mapping.containsKey(targetButtonIndex)) {
            String mappedKey = mapping.get(targetButtonIndex);
            return mappedKey != null && Boolean.TRUE.equals(snapshot.buttons().get(mappedKey));
        }
        return isButtonPressed(snapshot.buttons(), targetButtonIndex);
    }

    public String getLogicalButtonLabel(JoystickSnapshot snapshot, int targetButtonIndex) {
        Map<Integer, String> mapping = getManualMappingForController(snapshot.controllerName());
        String mappedKey = mapping.get(targetButtonIndex);
        Integer mappedPhysicalIndex = parseButtonIndex(mappedKey);
        if (mappedKey != null && !mappedKey.isBlank() && (mappedPhysicalIndex == null || mappedPhysicalIndex != targetButtonIndex)) {
            String normalizedMapped = mappedPhysicalIndex == null ? mappedKey : "B" + mappedPhysicalIndex;
            return "B" + targetButtonIndex + "←" + normalizedMapped;
        }
        return "B" + targetButtonIndex;
    }

    public Integer resolveLogicalButtonForPhysicalKey(JoystickSnapshot snapshot, String physicalButtonKey) {
        if (physicalButtonKey == null || physicalButtonKey.isBlank()) {
            return null;
        }
        Map<Integer, String> mapping = getManualMappingForController(snapshot.controllerName());
        for (Map.Entry<Integer, String> entry : mapping.entrySet()) {
            if (physicalButtonKey.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return parseButtonIndex(physicalButtonKey);
    }

    public void resetToDefaults() {
        preferredInputDevice = PreferredInputDevice.AUTO;
        joystickInput.setManualControllerSelection(null);

        pitchAxis = JoystickAxisOption.Y;
        yawAxis = JoystickAxisOption.X;
        rollAxis = JoystickAxisOption.RZ;
        throttleAxis = JoystickAxisOption.SLIDER;

        invertPitch = false;
        invertYaw = false;
        invertRoll = false;
        invertThrottle = false;

        triggerButtonIndex = 0;
        boostButtonIndex = 1;
        triggerButtonAction = JoystickButtonAction.FIRE_PRIMARY;
        solidPlaneEnabled = true;
        debugModeEnabled = false;

        manualButtonMappings.clear();
        onSettingsChanged();
    }

    private void applyKeyboard(ShipState shipState, KeyboardInput.KeyboardSnapshot input, double deltaTimeSec) {
        double angleStep = ANGULAR_SPEED_DEG_PER_SEC * deltaTimeSec;
        double throttleStep = THROTTLE_UNITS_PER_SEC * deltaTimeSec;
        double centerStep = AUTO_CENTER_DEG_PER_SEC * deltaTimeSec;

        if (input.up()) {
            shipState.addPitchTarget(-angleStep);
        }
        if (input.down()) {
            shipState.addPitchTarget(angleStep);
        }
        if (!input.up() && !input.down()) {
            shipState.nudgePitchTargetTowardCenter(centerStep);
        }

        if (input.left()) {
            shipState.addYawTarget(-angleStep);
        }
        if (input.right()) {
            shipState.addYawTarget(angleStep);
        }
        if (!input.left() && !input.right()) {
            shipState.nudgeYawTargetTowardCenter(centerStep);
        }

        if (input.rollLeft()) {
            shipState.addRollTarget(-angleStep);
        }
        if (input.rollRight()) {
            shipState.addRollTarget(angleStep);
        }
        if (!input.rollLeft() && !input.rollRight()) {
            shipState.nudgeRollTargetTowardCenter(centerStep);
        }

        if (input.throttleUp()) {
            shipState.addThrottleTarget(throttleStep);
        }
        if (input.throttleDown()) {
            shipState.addThrottleTarget(-throttleStep);
        }
    }

    private void applyJoystick(ShipState shipState, JoystickSnapshot snapshot, double deltaTimeSec) {
        Map<String, Float> axes = snapshot.axes();
        firePrimaryActive = false;

        double pitch = withDeadzone(resolveAxisValue(axes, pitchAxis, "y", "y axis", "y-achsen"));
        double yaw = withDeadzone(resolveAxisValue(axes, yawAxis, "x", "x axis", "x-achsen"));
        double roll = withDeadzone(resolveAxisValue(axes, rollAxis, "rz", "z rotation", "z-rotation", "z", "z axis", "z-achsen"));
        double throttle = withDeadzone(resolveAxisValue(axes, throttleAxis, "slider", "throttle", "z"));

        pitch = applyInvert(pitch, invertPitch);
        yaw = applyInvert(yaw, invertYaw);
        roll = applyInvert(roll, invertRoll);
        throttle = applyInvert(throttle, invertThrottle);

        shipState.setPitchTargetDegrees(pitch * STARFOX_MAX_PITCH);
        shipState.setYawTargetDegrees(yaw * STARFOX_MAX_YAW);
        shipState.setRollTargetDegrees(roll * STARFOX_MAX_ROLL);

        if (throttleAxis != JoystickAxisOption.NONE && Math.abs(throttle) > 0.001) {
            shipState.setThrottleTarget((-throttle + 1.0) * 0.5);
        }

        boolean triggerPressed = isLogicalButtonPressed(snapshot, triggerButtonIndex);
        boolean boostPressed = isLogicalButtonPressed(snapshot, boostButtonIndex);
        if (triggerPressed) {
            applyTriggerAction(shipState, deltaTimeSec);
            firePrimaryActive = triggerButtonAction == JoystickButtonAction.FIRE_PRIMARY;
            boostActive = triggerButtonAction == JoystickButtonAction.BOOST;
        }
        if (boostPressed) {
            boostActive = true;
        }
    }

    private static double applyInvert(double value, boolean invert) {
        return invert ? -value : value;
    }

    private void applyTriggerAction(ShipState shipState, double deltaTimeSec) {
        switch (triggerButtonAction) {
            case NONE -> {
                // no-op
            }
            case BOOST -> {
                // Boost is a temporary +MPH visual/flight-feel overlay while held.
            }
            case FIRE_PRIMARY -> {
                // visual-only effect is rendered by HUD/ship panel while held
            }
        }
    }

    public boolean isFirePrimaryActive() {
        return firePrimaryActive;
    }

    public boolean isBoostActive() {
        return boostActive;
    }

    private static List<Integer> extractButtonIndices(Map<String, Boolean> buttons) {
        List<Integer> out = new ArrayList<>();
        for (String key : buttons.keySet()) {
            Integer parsed = parseButtonIndex(key);
            if (parsed != null && !out.contains(parsed)) {
                out.add(parsed);
            }
        }
        return out;
    }

    private static boolean isButtonPressed(Map<String, Boolean> buttons, int targetButtonIndex) {
        for (Map.Entry<String, Boolean> entry : buttons.entrySet()) {
            if (!entry.getValue()) {
                continue;
            }
            Integer parsed = parseButtonIndex(entry.getKey());
            if (parsed != null && parsed == targetButtonIndex) {
                return true;
            }
        }
        return false;
    }

    private static Integer parseButtonIndex(String raw) {
        if (raw == null) {
            return null;
        }

        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }

        if (digits.length() == 0) {
            return null;
        }

        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static double withDeadzone(double value) {
        return Math.abs(value) < JOYSTICK_DEADZONE ? 0.0 : value;
    }

    private static double resolveAxisValue(Map<String, Float> axes, JoystickAxisOption option, String... autoAliases) {
        return switch (option) {
            case AUTO -> readAxis(axes, autoAliases);
            case X -> readAxis(axes, "x", "x axis", "x-achsen");
            case Y -> readAxis(axes, "y", "y axis", "y-achsen");
            case Z -> readAxis(axes, "z", "z axis", "z-achsen");
            case RZ -> readAxis(axes, "rz", "z rotation", "z-rotation");
            case SLIDER -> readAxis(axes, "slider", "throttle");
            case NONE -> 0.0;
        };
    }

    private static double readAxis(Map<String, Float> axes, String... aliases) {
        for (Map.Entry<String, Float> entry : axes.entrySet()) {
            String key = entry.getKey().toLowerCase();
            for (String alias : aliases) {
                String needle = alias.toLowerCase();
                if (key.equals(needle) || key.contains(needle)) {
                    return entry.getValue();
                }
            }
        }
        return 0.0;
    }

    private void onSettingsChanged() {
        if (!suppressSettingsChangeNotifications) {
            settingsChangedListener.run();
        }
    }
}
