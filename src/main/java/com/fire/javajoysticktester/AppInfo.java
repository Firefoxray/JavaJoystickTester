package com.fire.javajoysticktester;

/**
 * Application identity constants.
 */
public final class AppInfo {
    public static final String VERSION = "0.8";
    public static final String TITLE = "Java Joystick Tester";

    private AppInfo() {
        // constants only
    }

    public static String fullTitle() {
        return TITLE + " (" + VERSION + ")";
    }
}
