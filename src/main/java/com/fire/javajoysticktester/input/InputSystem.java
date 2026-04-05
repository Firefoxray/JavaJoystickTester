package com.fire.javajoysticktester.input;

import com.fire.javajoysticktester.model.ShipState;

import java.util.Map;

/**
 * Applies keyboard + joystick input to ShipState through a shared flow.
 */
public class InputSystem {
    private static final double ANGULAR_SPEED_DEG_PER_SEC = 120.0;
    private static final double THROTTLE_UNITS_PER_SEC = 0.55;
    private static final double AUTO_CENTER_DEG_PER_SEC = 180.0;
    private static final double JOYSTICK_DEADZONE = 0.08;

    private final KeyboardInput keyboardInput;
    private final JoystickInput joystickInput;

    private PreferredInputDevice preferredInputDevice = PreferredInputDevice.AUTO;
    private JoystickSnapshot lastJoystickSnapshot = JoystickSnapshot.disconnected(java.util.List.of());

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
        } else {
            applyKeyboard(shipState, keyboardInput.snapshot(), deltaTimeSec);
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
        double angleStep = ANGULAR_SPEED_DEG_PER_SEC * deltaTimeSec;
        double throttleStep = THROTTLE_UNITS_PER_SEC * deltaTimeSec;

        Map<String, Float> axes = snapshot.axes();

        double x = readAxis(axes, "x", "x axis", "x-achsen");
        double y = readAxis(axes, "y", "y axis", "y-achsen");
        double rz = readAxis(axes, "rz", "z rotation", "z-rotation");
        double z = readAxis(axes, "z", "z axis", "z-achsen");
        double throttleAxis = readAxis(axes, "slider", "throttle", "z");

        x = withDeadzone(x);
        y = withDeadzone(y);
        rz = withDeadzone(rz);
        z = withDeadzone(z);
        throttleAxis = withDeadzone(throttleAxis);

        shipState.addPitchTarget(y * angleStep);
        shipState.addYawTarget(x * angleStep);
        shipState.addRollTarget((Math.abs(rz) > 0.001 ? rz : z) * angleStep);

        if (Math.abs(throttleAxis) > 0.001) {
            shipState.addThrottleTarget(-throttleAxis * throttleStep);
        }
    }

    private static double withDeadzone(double value) {
        return Math.abs(value) < JOYSTICK_DEADZONE ? 0.0 : value;
    }

    private static double readAxis(Map<String, Float> axes, String... aliases) {
        for (Map.Entry<String, Float> entry : axes.entrySet()) {
            String key = entry.getKey().toLowerCase();
            for (String alias : aliases) {
                if (key.equals(alias.toLowerCase()) || key.contains(alias.toLowerCase())) {
                    return entry.getValue();
                }
            }
        }
        return 0.0;
    }
}
