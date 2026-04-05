package com.fire.javajoysticktester.input;

import com.fire.javajoysticktester.model.ShipState;

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
    private int triggerButtonIndex = 0;
    private JoystickButtonAction triggerButtonAction = JoystickButtonAction.FIRE_PRIMARY;

    public InputSystem(KeyboardInput keyboardInput, JoystickInput joystickInput) {
        this.keyboardInput = keyboardInput;
        this.joystickInput = joystickInput;
    }

    public void update(ShipState shipState, double deltaTimeSec) {
        lastJoystickSnapshot = joystickInput.poll();

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

    public void setPreferredInputDevice(PreferredInputDevice preferredInputDevice) {
        this.preferredInputDevice = preferredInputDevice;
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

    public JoystickAxisOption getPitchAxis() {
        return pitchAxis;
    }

    public void setPitchAxis(JoystickAxisOption pitchAxis) {
        this.pitchAxis = pitchAxis;
    }

    public JoystickAxisOption getYawAxis() {
        return yawAxis;
    }

    public void setYawAxis(JoystickAxisOption yawAxis) {
        this.yawAxis = yawAxis;
    }

    public JoystickAxisOption getRollAxis() {
        return rollAxis;
    }

    public void setRollAxis(JoystickAxisOption rollAxis) {
        this.rollAxis = rollAxis;
    }

    public JoystickAxisOption getThrottleAxis() {
        return throttleAxis;
    }

    public void setThrottleAxis(JoystickAxisOption throttleAxis) {
        this.throttleAxis = throttleAxis;
    }

    public int getTriggerButtonIndex() {
        return triggerButtonIndex;
    }

    public void setTriggerButtonIndex(int triggerButtonIndex) {
        this.triggerButtonIndex = Math.max(0, triggerButtonIndex);
    }

    public JoystickButtonAction getTriggerButtonAction() {
        return triggerButtonAction;
    }

    public void setTriggerButtonAction(JoystickButtonAction triggerButtonAction) {
        this.triggerButtonAction = triggerButtonAction;
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

        double pitch = withDeadzone(resolveAxisValue(axes, pitchAxis, "y", "y axis", "y-achsen"));
        double yaw = withDeadzone(resolveAxisValue(axes, yawAxis, "x", "x axis", "x-achsen"));
        double roll = withDeadzone(resolveAxisValue(axes, rollAxis, "rz", "z rotation", "z-rotation", "z", "z axis", "z-achsen"));
        double throttle = withDeadzone(resolveAxisValue(axes, throttleAxis, "slider", "throttle", "z"));

        shipState.setPitchTargetDegrees(pitch * STARFOX_MAX_PITCH);
        shipState.setYawTargetDegrees(yaw * STARFOX_MAX_YAW);
        shipState.setRollTargetDegrees(roll * STARFOX_MAX_ROLL);

        if (throttleAxis != JoystickAxisOption.NONE && Math.abs(throttle) > 0.001) {
            shipState.setThrottleTarget((-throttle + 1.0) * 0.5);
        }

        if (isButtonPressed(snapshot.buttons(), triggerButtonIndex)) {
            applyTriggerAction(shipState, deltaTimeSec);
        }
    }

    private void applyTriggerAction(ShipState shipState, double deltaTimeSec) {
        switch (triggerButtonAction) {
            case NONE -> {
                // no-op
            }
            case BOOST -> shipState.addThrottleTarget(THROTTLE_UNITS_PER_SEC * deltaTimeSec * 2.5);
            case FIRE_PRIMARY -> shipState.addRollTarget(18.0 * deltaTimeSec);
        }
    }

    private static boolean isButtonPressed(Map<String, Boolean> buttons, int targetButtonIndex) {
        String numericKey = Integer.toString(targetButtonIndex);
        for (Map.Entry<String, Boolean> entry : buttons.entrySet()) {
            if (!entry.getValue()) {
                continue;
            }
            String normalized = entry.getKey().trim().toLowerCase();
            if (normalized.equals(numericKey)
                    || normalized.equals("button " + numericKey)
                    || normalized.endsWith(" " + numericKey)) {
                return true;
            }
        }
        return false;
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
}
