package com.fire.javajoysticktester.input;

public enum JoystickAxisOption {
    AUTO("Auto"),
    X("X"),
    Y("Y"),
    Z("Z"),
    RZ("RZ"),
    SLIDER("Slider"),
    NONE("None");

    private final String label;

    JoystickAxisOption(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
