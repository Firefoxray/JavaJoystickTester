package com.fire.javajoysticktester.ui;

import com.fire.javajoysticktester.input.InputSystem;
import com.fire.javajoysticktester.input.JoystickAxisOption;
import com.fire.javajoysticktester.input.JoystickButtonAction;
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
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.BorderLayout;
import java.util.List;
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

        JMenu joystickMenu = new JMenu("Joystick Controller");
        joystickMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                rebuildJoystickSelectionMenu(joystickMenu);
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                // no-op
            }

            @Override
            public void menuCanceled(MenuEvent e) {
                // no-op
            }
        });

        JMenu mappingMenu = new JMenu("Joystick Mapping");
        addAxisBindingMenu(mappingMenu, "Pitch Axis", inputSystem::getPitchAxis, inputSystem::setPitchAxis);
        addAxisBindingMenu(mappingMenu, "Yaw Axis", inputSystem::getYawAxis, inputSystem::setYawAxis);
        addAxisBindingMenu(mappingMenu, "Roll Axis", inputSystem::getRollAxis, inputSystem::setRollAxis);
        addAxisBindingMenu(mappingMenu, "Throttle Axis", inputSystem::getThrottleAxis, inputSystem::setThrottleAxis);

        JMenu triggerButtonMenu = new JMenu("Trigger Button");
        ButtonGroup triggerButtons = new ButtonGroup();
        for (int i = 0; i <= 7; i++) {
            int index = i;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem("Button " + i, i == inputSystem.getTriggerButtonIndex());
            item.addActionListener(e -> inputSystem.setTriggerButtonIndex(index));
            triggerButtons.add(item);
            triggerButtonMenu.add(item);
        }
        mappingMenu.add(triggerButtonMenu);

        JMenu triggerActionMenu = new JMenu("Trigger Action");
        ButtonGroup triggerActions = new ButtonGroup();
        for (JoystickButtonAction action : JoystickButtonAction.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(action.name(), action == inputSystem.getTriggerButtonAction());
            item.addActionListener(e -> inputSystem.setTriggerButtonAction(action));
            triggerActions.add(item);
            triggerActionMenu.add(item);
        }
        mappingMenu.add(triggerActionMenu);

        JMenuItem controlsItem = new JMenuItem("Controls & Input Status...");
        controlsItem.addActionListener(e -> showControlsDialog());

        settingsMenu.add(inputMenu);
        settingsMenu.add(joystickMenu);
        settingsMenu.add(mappingMenu);
        settingsMenu.add(controlsItem);
        menuBar.add(settingsMenu);

        return menuBar;
    }

    private void addAxisBindingMenu(JMenu parent,
                                    String title,
                                    java.util.function.Supplier<JoystickAxisOption> getter,
                                    java.util.function.Consumer<JoystickAxisOption> setter) {
        JMenu axisMenu = new JMenu(title);
        ButtonGroup group = new ButtonGroup();
        for (JoystickAxisOption option : JoystickAxisOption.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(option.label(), getter.get() == option);
            item.addActionListener(e -> setter.accept(option));
            group.add(item);
            axisMenu.add(item);
        }
        parent.add(axisMenu);
    }

    private JRadioButtonMenuItem addInputSelection(ButtonGroup group, String label, PreferredInputDevice device, boolean selected) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, selected);
        item.addActionListener(e -> inputSystem.setPreferredInputDevice(device));
        group.add(item);
        return item;
    }

    private void rebuildJoystickSelectionMenu(JMenu joystickMenu) {
        joystickMenu.removeAll();

        ButtonGroup group = new ButtonGroup();
        String manualSelection = inputSystem.getSelectedJoystickName();
        String activeController = inputSystem.getLastJoystickSnapshot().controllerName();

        JRadioButtonMenuItem autoSelect = new JRadioButtonMenuItem("Auto-select (prefer T.16000M)", manualSelection == null);
        autoSelect.addActionListener(e -> inputSystem.setSelectedJoystickName(null));
        group.add(autoSelect);
        joystickMenu.add(autoSelect);

        joystickMenu.addSeparator();

        List<String> names = inputSystem.getDetectedJoystickNames();
        if (names.isEmpty()) {
            JMenuItem none = new JMenuItem("No controllers detected yet");
            none.setEnabled(false);
            joystickMenu.add(none);
            return;
        }

        for (String name : names) {
            boolean isActive = name.equals(activeController);
            String label = isActive ? name + "  [ACTIVE]" : name;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, name.equals(manualSelection));
            item.addActionListener(e -> inputSystem.setSelectedJoystickName(name));
            group.add(item);
            joystickMenu.add(item);
        }
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
        text.append(" - Active input: ").append(inputSystem.getActiveInputDescription()).append("\n");
        text.append(" - Keyboard active: ").append(inputSystem.isKeyboardActive() ? "YES" : "NO").append("\n");
        text.append(" - Joystick connected: ").append(snapshot.connected() ? "YES" : "NO").append("\n");
        text.append(" - Joystick access: ").append(snapshot.accessStatus()).append("\n");
        text.append(" - Active controller: ").append(snapshot.controllerName()).append("\n");
        text.append(" - Controller selection: ").append(inputSystem.getSelectedJoystickName() == null ? "AUTO" : inputSystem.getSelectedJoystickName()).append("\n");
        text.append(" - T.16000M detected: ").append(snapshot.thrustmasterT16000MDetected() ? "YES" : "NO").append("\n");

        text.append("\nJoystick mapping:\n");
        text.append(" - Pitch axis: ").append(inputSystem.getPitchAxis().label()).append("\n");
        text.append(" - Yaw axis: ").append(inputSystem.getYawAxis().label()).append("\n");
        text.append(" - Roll axis: ").append(inputSystem.getRollAxis().label()).append("\n");
        text.append(" - Throttle axis: ").append(inputSystem.getThrottleAxis().label()).append("\n");
        text.append(" - Trigger: Button ").append(inputSystem.getTriggerButtonIndex())
                .append(" -> ").append(inputSystem.getTriggerButtonAction()).append("\n");

        if (snapshot.linuxPermissionDenied()) {
            text.append("\nLinux hint: your user may need active-seat ACLs (logind/udev) or input-group access for /dev/input/event*.\n");
        }

        if (!snapshot.allDetectedControllerNames().isEmpty()) {
            text.append("\nDetected controllers:\n");
            for (String name : snapshot.allDetectedControllerNames()) {
                String marker = name.equals(snapshot.controllerName()) ? " [ACTIVE]" : "";
                text.append(" - ").append(name).append(marker).append("\n");
            }
        }

        JTextArea area = new JTextArea(text.toString());
        area.setEditable(false);
        area.setRows(24);
        area.setColumns(60);

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
            shipPanel.updateInputDebug(inputSystem.getPreferredInputDevice(), inputSystem.isKeyboardActive(), inputSystem.getLastJoystickSnapshot(), inputSystem.getActiveInputDescription());
            shipPanel.repaint();
        });

        timer.setCoalesce(true);
        timer.start();
    }
}
