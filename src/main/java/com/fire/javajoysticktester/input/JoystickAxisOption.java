package com.fire.javajoysticktester.input;

public enum JoystickAxisOption {
    AUTO("Auto (use common flight-stick defaults)"),
    X("Raw X axis (stick left/right)"),
    Y("Raw Y axis (stick forward/back, usually pitch)"),
    Z("Raw Z axis (often throttle/slider)"),
    RZ("Raw RZ axis (often twist yaw)"),
    SLIDER("Raw Slider axis (throttle)"),
    NONE("None");

    private final String label;

    JoystickAxisOption(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
