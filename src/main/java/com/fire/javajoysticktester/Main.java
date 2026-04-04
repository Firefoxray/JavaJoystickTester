package com.fire.javajoysticktester;

import com.fire.javajoysticktester.ui.MainWindow;

import javax.swing.SwingUtilities;

/**
 * Application entry point.
 */
public final class Main {
    private Main() {
        // Utility class; do not instantiate.
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.showWindow();
        });
    }
}
