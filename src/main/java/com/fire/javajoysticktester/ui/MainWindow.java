package com.fire.javajoysticktester.ui;

import com.fire.javajoysticktester.input.InputSystem;
import com.fire.javajoysticktester.input.JoystickInput;
import com.fire.javajoysticktester.input.JoystickSnapshot;
import com.fire.javajoysticktester.input.KeyboardInput;
import com.fire.javajoysticktester.input.PreferredInputDevice;
import com.fire.javajoysticktester.model.ShipState;
import com.fire.javajoysticktester.render.ShipPanel;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.util.Map;

/**
 * Main application frame.
 */
public class MainWindow {
    private static final String APP_TITLE = "Java Joystick Tester (0.1 Alpha)";
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int TARGET_FPS = 60;

    private final JFrame frame;
    private final ShipState shipState;
    private final ShipPanel shipPanel;
    private final KeyboardInput keyboardInput;
    private final InputSystem inputSystem;

    private long lastUpdateNanos;

    public MainWindow() {
        frame = new JFrame(APP_TITLE);
        shipState = new ShipState();
        shipPanel = new ShipPanel(shipState);
        keyboardInput = new KeyboardInput();
        inputSystem = new InputSystem(keyboardInput, new JoystickInput());

        keyboardInput.install(shipPanel);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setJMenuBar(buildMenuBar());
        frame.add(shipPanel, BorderLayout.CENTER);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
    }

    public void showWindow() {
        frame.setVisible(true);
        shipPanel.requestFocusInWindow();
        startLoop();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu settingsMenu = new JMenu("Settings");

        JMenu inputMenu = new JMenu("Preferred Input");
        ButtonGroup group = new ButtonGroup();
        inputMenu.add(addInputSelection(group, "Auto", PreferredInputDevice.AUTO, true));
        inputMenu.add(addInputSelection(group, "Keyboard", PreferredInputDevice.KEYBOARD, false));
        inputMenu.add(addInputSelection(group, "Joystick", PreferredInputDevice.JOYSTICK, false));

        JMenuItem controlsItem = new JMenuItem("Controls & Input Status...");
        controlsItem.addActionListener(e -> showControlsDialog());

        settingsMenu.add(inputMenu);
        settingsMenu.add(controlsItem);
        menuBar.add(settingsMenu);

        return menuBar;
    }

    private JRadioButtonMenuItem addInputSelection(ButtonGroup group, String label, PreferredInputDevice device, boolean selected) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, selected);
        item.addActionListener(e -> inputSystem.setPreferredInputDevice(device));
        group.add(item);
        return item;
    }

    private void showControlsDialog() {
        JoystickSnapshot snapshot = inputSystem.getLastJoystickSnapshot();
        StringBuilder text = new StringBuilder();
        text.append("Java Joystick Tester (0.1 Alpha)\n\n");
        text.append("Keyboard bindings:\n");
        for (Map.Entry<String, String> binding : KeyboardInput.getBindingDescriptions().entrySet()) {
            text.append(" - ").append(binding.getKey()).append(": ").append(binding.getValue()).append("\n");
        }

        text.append("\nInput status:\n");
        text.append(" - Preferred input: ").append(inputSystem.getPreferredInputDevice()).append("\n");
        text.append(" - Keyboard active: ").append(inputSystem.isKeyboardActive() ? "YES" : "NO").append("\n");
        text.append(" - Joystick connected: ").append(snapshot.connected() ? "YES" : "NO").append("\n");
        text.append(" - Active controller: ").append(snapshot.controllerName()).append("\n");
        text.append(" - T.16000M detected: ").append(snapshot.thrustmasterT16000MDetected() ? "YES" : "NO").append("\n");

        if (!snapshot.allDetectedControllerNames().isEmpty()) {
            text.append("\nDetected controllers:\n");
            for (String name : snapshot.allDetectedControllerNames()) {
                text.append(" - ").append(name).append("\n");
            }
        }

        JTextArea area = new JTextArea(text.toString());
        area.setEditable(false);
        area.setRows(18);
        area.setColumns(52);

        JOptionPane.showMessageDialog(frame, area, "Controls & Input Status", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startLoop() {
        int delayMs = 1000 / TARGET_FPS;
        lastUpdateNanos = System.nanoTime();

        Timer timer = new Timer(delayMs, event -> {
            long now = System.nanoTime();
            double deltaSec = (now - lastUpdateNanos) / 1_000_000_000.0;
            lastUpdateNanos = now;

            inputSystem.update(shipState, deltaSec);
            shipState.update(deltaSec);
            shipPanel.updateInputDebug(inputSystem.getPreferredInputDevice(), inputSystem.isKeyboardActive(), inputSystem.getLastJoystickSnapshot());
            shipPanel.repaint();
        });

        timer.setCoalesce(true);
        timer.start();
    }
}
