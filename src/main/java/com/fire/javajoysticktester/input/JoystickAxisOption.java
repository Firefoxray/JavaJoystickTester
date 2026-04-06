package com.fire.javajoysticktester.input;

public enum JoystickAxisOption {
    AUTO("Auto"),
    X("X (left/right)"),
    Y("Y (forward/back)"),
    Z("Z (throttle/slider on many devices)"),
    RZ("RZ (twist/yaw on many flight sticks)"),
    SLIDER("Slider/Throttle"),
    NONE("None");

    private final String label;

    JoystickAxisOption(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
