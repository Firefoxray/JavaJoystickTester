package com.fire.javajoysticktester.ui;

import com.fire.javajoysticktester.AppInfo;
import com.fire.javajoysticktester.input.InputSystem;
import com.fire.javajoysticktester.input.JoystickAxisOption;
import com.fire.javajoysticktester.input.JoystickButtonAction;
import com.fire.javajoysticktester.input.JoystickInput;
import com.fire.javajoysticktester.input.JoystickSnapshot;
import com.fire.javajoysticktester.input.KeyboardInput;
import com.fire.javajoysticktester.input.PreferredInputDevice;
import com.fire.javajoysticktester.model.ShipState;
import com.fire.javajoysticktester.render.ShipPanel;
import com.fire.javajoysticktester.update.GitUpdateService;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application frame.
 */
public class MainWindow {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int TARGET_FPS = 60;

    private final JFrame frame;
    private final ShipState shipState;
    private final ShipPanel shipPanel;
    private final KeyboardInput keyboardInput;
    private final InputSystem inputSystem;
    private final GitUpdateService gitUpdateService;
    private final AppConfigManager configManager;

    private long lastUpdateNanos;

    public MainWindow() {
        frame = new JFrame(AppInfo.fullTitle());
        shipState = new ShipState();
        shipPanel = new ShipPanel(shipState);
        keyboardInput = new KeyboardInput();
        inputSystem = new InputSystem(keyboardInput, new JoystickInput());
        gitUpdateService = new GitUpdateService();
        configManager = new AppConfigManager();

        inputSystem.setSettingsChangedListener(() -> configManager.saveFrom(inputSystem));
        configManager.loadInto(inputSystem);

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
        inputMenu.add(addInputSelection(group, "Auto", PreferredInputDevice.AUTO, inputSystem.getPreferredInputDevice() == PreferredInputDevice.AUTO));
        inputMenu.add(addInputSelection(group, "Keyboard", PreferredInputDevice.KEYBOARD, inputSystem.getPreferredInputDevice() == PreferredInputDevice.KEYBOARD));
        inputMenu.add(addInputSelection(group, "Joystick", PreferredInputDevice.JOYSTICK, inputSystem.getPreferredInputDevice() == PreferredInputDevice.JOYSTICK));

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
        addAxisBindingMenu(mappingMenu, "Pitch (Nose Up/Down) Axis", inputSystem::getPitchAxis, inputSystem::setPitchAxis);
        addAxisBindingMenu(mappingMenu, "Yaw (Nose Left/Right) Axis", inputSystem::getYawAxis, inputSystem::setYawAxis);
        addAxisBindingMenu(mappingMenu, "Roll (Bank Left/Right) Axis", inputSystem::getRollAxis, inputSystem::setRollAxis);
        addAxisBindingMenu(mappingMenu, "Throttle / Speed Axis", inputSystem::getThrottleAxis, inputSystem::setThrottleAxis);

        JMenu invertMenu = new JMenu("Invert Axes");
        invertMenu.add(addInvertToggle("Invert Pitch", inputSystem::isInvertPitch, inputSystem::setInvertPitch));
        invertMenu.add(addInvertToggle("Invert Yaw", inputSystem::isInvertYaw, inputSystem::setInvertYaw));
        invertMenu.add(addInvertToggle("Invert Roll", inputSystem::isInvertRoll, inputSystem::setInvertRoll));
        invertMenu.add(addInvertToggle("Invert Throttle", inputSystem::isInvertThrottle, inputSystem::setInvertThrottle));
        mappingMenu.addSeparator();
        mappingMenu.add(invertMenu);

        JMenu triggerButtonMenu = new JMenu("Trigger Button");
        triggerButtonMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                rebuildTriggerButtonMenu(triggerButtonMenu);
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
        mappingMenu.add(triggerButtonMenu);
        JMenuItem quickTriggerMapItem = new JMenuItem("Remap Trigger Button...");
        quickTriggerMapItem.addActionListener(e -> startSingleButtonRemap("Trigger", inputSystem.getTriggerButtonIndex(), inputSystem::setTriggerButtonIndex));
        mappingMenu.add(quickTriggerMapItem);

        JMenu boostButtonMenu = new JMenu("Boost Button");
        boostButtonMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                rebuildBoostButtonMenu(boostButtonMenu);
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
        mappingMenu.add(boostButtonMenu);
        JMenuItem quickBoostMapItem = new JMenuItem("Remap Boost Button...");
        quickBoostMapItem.addActionListener(e -> startSingleButtonRemap("Boost", inputSystem.getBoostButtonIndex(), inputSystem::setBoostButtonIndex));
        mappingMenu.add(quickBoostMapItem);

        JMenu triggerActionMenu = new JMenu("Trigger Action");
        ButtonGroup triggerActions = new ButtonGroup();
        for (JoystickButtonAction action : JoystickButtonAction.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(action.name(), action == inputSystem.getTriggerButtonAction());
            item.addActionListener(e -> inputSystem.setTriggerButtonAction(action));
            triggerActions.add(item);
            triggerActionMenu.add(item);
        }
        mappingMenu.add(triggerActionMenu);

        JMenu manualMapButtonsItem = new JMenu("Remap Logical Buttons");
        rebuildPerButtonRemapMenu(manualMapButtonsItem);
        manualMapButtonsItem.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                rebuildPerButtonRemapMenu(manualMapButtonsItem);
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
        mappingMenu.addSeparator();
        mappingMenu.add(manualMapButtonsItem);

        JMenuItem clearManualMapItem = new JMenuItem("Clear Manual Button Mapping");
        clearManualMapItem.addActionListener(e -> clearManualButtonMapping());
        mappingMenu.add(clearManualMapItem);

        JMenuItem controlsItem = new JMenuItem("Controls & Input Status...");
        controlsItem.addActionListener(e -> showControlsDialog());

        JCheckBoxMenuItem debugModeItem = new JCheckBoxMenuItem("Debug Mode", inputSystem.isDebugModeEnabled());
        debugModeItem.addActionListener(e -> {
            boolean enabled = debugModeItem.isSelected();
            inputSystem.setDebugModeEnabled(enabled);
            inputSystem.setSolidPlaneEnabled(!enabled);
        });

        JMenuItem resetDefaultsItem = new JMenuItem("Reset to Defaults...");
        resetDefaultsItem.addActionListener(e -> resetToDefaults());

        settingsMenu.add(inputMenu);
        settingsMenu.add(joystickMenu);
        settingsMenu.add(mappingMenu);
        settingsMenu.add(debugModeItem);
        settingsMenu.add(controlsItem);
        settingsMenu.addSeparator();
        settingsMenu.add(resetDefaultsItem);
        menuBar.add(settingsMenu);

        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem solidPlaneItem = new JCheckBoxMenuItem("Enable Solid Plane (retro fill)", inputSystem.isSolidPlaneEnabled());
        solidPlaneItem.addActionListener(e -> inputSystem.setSolidPlaneEnabled(solidPlaneItem.isSelected()));
        viewMenu.add(solidPlaneItem);
        menuBar.add(viewMenu);

        JMenu extrasMenu = new JMenu("Extras");
        JMenuItem checkUpdatesItem = new JMenuItem("Check for Updates...");
        checkUpdatesItem.addActionListener(e -> checkForUpdatesFromMenu());
        JMenuItem installDesktopLauncherItem = new JMenuItem("Install Desktop Launcher");
        installDesktopLauncherItem.addActionListener(e -> installDesktopLauncher());
        extrasMenu.add(checkUpdatesItem);
        extrasMenu.addSeparator();
        extrasMenu.add(installDesktopLauncherItem);
        menuBar.add(extrasMenu);

        return menuBar;
    }

    private JCheckBoxMenuItem addInvertToggle(String label,
                                              java.util.function.BooleanSupplier getter,
                                              java.util.function.Consumer<Boolean> setter) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, getter.getAsBoolean());
        item.addActionListener(e -> setter.accept(item.isSelected()));
        return item;
    }

    private void installDesktopLauncher() {
        Path projectRoot = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath();
        DesktopLauncherInstaller.InstallResult result = DesktopLauncherInstaller.installForCurrentUser(projectRoot);
        int messageType = result.success() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
        String title = result.success() ? "Desktop Launcher Installed" : "Desktop Launcher Installation Failed";
        JOptionPane.showMessageDialog(frame, result.message(), title, messageType);
    }

    private void checkForUpdatesFromMenu() {
        runAsync("Checking for updates...", () -> {
            GitUpdateService.CheckResult result = gitUpdateService.checkForUpdates();
            SwingUtilities.invokeLater(() -> handleUpdateCheckResult(result));
        });
    }

    private void handleUpdateCheckResult(GitUpdateService.CheckResult result) {
        String header = "Branch: " + result.branch() + "\n"
                + "Commit: " + result.shortCommit() + "\n"
                + "Status: " + formatStatus(result.status()) + "\n\n";

        if (result.status() == GitUpdateService.UpdateStatus.UPDATE_AVAILABLE) {
            int choice = JOptionPane.showConfirmDialog(
                    frame,
                    header + result.message() + "\n\nUpdate now and restart?",
                    "Updates",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                runAsync("Updating and restarting...", () -> {
                    GitUpdateService.UpdateResult updateResult = gitUpdateService.updateAndRestart();
                    SwingUtilities.invokeLater(() -> {
                        if (updateResult.success()) {
                            frame.dispose();
                            System.exit(0);
                        } else {
                            JOptionPane.showMessageDialog(frame, updateResult.message(), "Update failed", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                });
            }
            return;
        }

        int messageType = result.status() == GitUpdateService.UpdateStatus.UNABLE_TO_CHECK
                ? JOptionPane.WARNING_MESSAGE
                : JOptionPane.INFORMATION_MESSAGE;

        JOptionPane.showMessageDialog(frame, header + result.message(), "Updates", messageType);
    }

    private static String formatStatus(GitUpdateService.UpdateStatus status) {
        return switch (status) {
            case ALREADY_UP_TO_DATE -> "Already up to date";
            case UPDATE_AVAILABLE -> "Update available";
            case WORKING_TREE_DIRTY -> "Working tree has local changes";
            case UNABLE_TO_CHECK -> "Unable to check for updates";
        };
    }

    private void runAsync(String label, Runnable task) {
        frame.setTitle(AppInfo.fullTitle() + " - " + label);
        Thread thread = new Thread(() -> {
            try {
                task.run();
            } finally {
                SwingUtilities.invokeLater(() -> frame.setTitle(AppInfo.fullTitle()));
            }
        }, "update-check-thread");
        thread.setDaemon(true);
        thread.start();
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

    private void rebuildTriggerButtonMenu(JMenu triggerButtonMenu) {
        triggerButtonMenu.removeAll();
        ButtonGroup group = new ButtonGroup();

        List<Integer> indices = inputSystem.getLogicalButtonIndices(inputSystem.getLastJoystickSnapshot());
        for (Integer index : indices) {
            int buttonIndex = index;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem("Button " + buttonIndex, buttonIndex == inputSystem.getTriggerButtonIndex());
            item.addActionListener(e -> inputSystem.setTriggerButtonIndex(buttonIndex));
            group.add(item);
            triggerButtonMenu.add(item);
        }
    }

    private void rebuildBoostButtonMenu(JMenu boostButtonMenu) {
        boostButtonMenu.removeAll();
        ButtonGroup group = new ButtonGroup();

        List<Integer> indices = inputSystem.getLogicalButtonIndices(inputSystem.getLastJoystickSnapshot());
        for (Integer index : indices) {
            int buttonIndex = index;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem("Button " + buttonIndex, buttonIndex == inputSystem.getBoostButtonIndex());
            item.addActionListener(e -> inputSystem.setBoostButtonIndex(buttonIndex));
            group.add(item);
            boostButtonMenu.add(item);
        }
    }

    private void rebuildPerButtonRemapMenu(JMenu remapMenu) {
        remapMenu.removeAll();
        for (int logicalButton = 0; logicalButton < 16; logicalButton++) {
            int logical = logicalButton;
            JMenuItem item = new JMenuItem("Remap Logical Button " + logical + "...");
            item.addActionListener(e -> startPerLogicalRemap(logical));
            remapMenu.add(item);
        }
    }

    private void startPerLogicalRemap(int logicalButton) {
        startSingleButtonRemap("Logical Button " + logicalButton, logicalButton, selectedLogical -> {
            JoystickSnapshot snapshot = inputSystem.getLastJoystickSnapshot();
            if (!snapshot.connected()) {
                return;
            }
            Map<Integer, String> existing = new LinkedHashMap<>(inputSystem.getManualMappingForController(snapshot.controllerName()));
            String physicalKey = "Button " + selectedLogical;
            existing.put(logicalButton, physicalKey);
            inputSystem.setManualMappingForController(snapshot.controllerName(), existing);
        });
    }

    private void startSingleButtonRemap(String mappingName, int logicalButtonPrompt, java.util.function.IntConsumer resultConsumer) {
        JoystickSnapshot snapshot = inputSystem.getLastJoystickSnapshot();
        if (!snapshot.connected()) {
            JOptionPane.showMessageDialog(frame,
                    "Connect/select a joystick first.",
                    mappingName + " Remap",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        javax.swing.JDialog dialog = new javax.swing.JDialog(frame, "Listening: " + mappingName, Dialog.ModalityType.MODELESS);
        dialog.setLayout(new BorderLayout(8, 8));

        javax.swing.JLabel label = new javax.swing.JLabel("Press the physical button for Button " + logicalButtonPrompt + "...");
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 0, 12));
        dialog.add(label, BorderLayout.NORTH);

        javax.swing.JLabel hint = new javax.swing.JLabel("Capturing only the next newly pressed button (Esc/Cancel to stop)");
        hint.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 4, 12));
        dialog.add(hint, BorderLayout.CENTER);

        final java.util.Set<String> previousPressed = new java.util.HashSet<>(pressedButtonKeys(inputSystem.pollJoystickSnapshotNow()));
        final long deadlineMillis = System.currentTimeMillis() + 8000;
        final javax.swing.Timer pollTimer = new Timer(35, null);

        javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
        cancelButton.addActionListener(e -> {
            pollTimer.stop();
            dialog.dispose();
        });
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getRootPane().registerKeyboardAction(
                e -> {
                    pollTimer.stop();
                    dialog.dispose();
                },
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(frame);

        pollTimer.addActionListener(e -> {
            if (System.currentTimeMillis() >= deadlineMillis) {
                pollTimer.stop();
                dialog.dispose();
                JOptionPane.showMessageDialog(frame,
                        "No button press detected in time.",
                        mappingName + " Remap",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            JoystickSnapshot live = inputSystem.pollJoystickSnapshotNow();
            for (String key : pressedButtonKeys(live)) {
                if (!previousPressed.contains(key)) {
                    Integer detectedLogical = inputSystem.resolveLogicalButtonForPhysicalKey(live, key);
                    if (detectedLogical != null) {
                        pollTimer.stop();
                        dialog.dispose();
                        resultConsumer.accept(detectedLogical);
                        JOptionPane.showMessageDialog(frame,
                                mappingName + " mapped to logical Button " + detectedLogical + " (" + key + ").",
                                mappingName + " Remap",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }
            }
            previousPressed.clear();
            previousPressed.addAll(pressedButtonKeys(live));
        });
        pollTimer.start();
        dialog.setVisible(true);
    }

    private static List<String> pressedButtonKeys(JoystickSnapshot snapshot) {
        List<String> pressed = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : snapshot.buttons().entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                pressed.add(entry.getKey());
            }
        }
        return pressed;
    }

    private void clearManualButtonMapping() {
        String controller = inputSystem.getLastJoystickSnapshot().controllerName();
        if (controller == null || controller.equals("None")) {
            JOptionPane.showMessageDialog(frame,
                    "No active controller selected.",
                    "Clear Manual Mapping",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                frame,
                "Clear manual button mapping for:\n" + controller + "?",
                "Clear Manual Mapping",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            inputSystem.setManualMappingForController(controller, Map.of());
        }
    }

    private void resetToDefaults() {
        int choice = JOptionPane.showConfirmDialog(
                frame,
                "Reset all settings to defaults?\n"
                        + "This clears preferred input, controller selection, axis/trigger mappings,\n"
                        + "invert flags, manual button maps, visual toggles, and saved config values.",
                "Reset to Defaults",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        inputSystem.resetToDefaults();
        configManager.resetConfigFile();
        configManager.saveFrom(inputSystem);

        JOptionPane.showMessageDialog(frame,
                "Settings reset to defaults.\nConfig file: " + configManager.getConfigPath(),
                "Reset Complete",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showControlsDialog() {
        JoystickSnapshot snapshot = inputSystem.getLastJoystickSnapshot();
        StringBuilder text = new StringBuilder();
        text.append(AppInfo.fullTitle()).append("\n\n");
        text.append("Keyboard bindings:\n");
        for (Map.Entry<String, String> binding : KeyboardInput.getBindingDescriptions().entrySet()) {
            text.append(" - ").append(binding.getKey()).append(": ").append(binding.getValue()).append("\n");
        }
        text.append("\nFlight control quick guide:\n");
        text.append(" - Pitch = nose up/down\n");
        text.append(" - Yaw = nose left/right\n");
        text.append(" - Roll = bank left/right\n");
        text.append(" - Throttle = speed control (100 to 1000 MPH HUD range)\n");

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
        text.append(" - Pitch axis (nose up/down): ").append(inputSystem.getPitchAxis().label())
                .append(inputSystem.isInvertPitch() ? " (inverted)" : "").append("\n");
        text.append(" - Yaw axis (nose left/right): ").append(inputSystem.getYawAxis().label())
                .append(inputSystem.isInvertYaw() ? " (inverted)" : "").append("\n");
        text.append(" - Roll axis (bank left/right): ").append(inputSystem.getRollAxis().label())
                .append(inputSystem.isInvertRoll() ? " (inverted)" : "").append("\n");
        text.append(" - Throttle axis (speed): ").append(inputSystem.getThrottleAxis().label())
                .append(inputSystem.isInvertThrottle() ? " (inverted)" : "").append("\n");
        text.append(" - Trigger: Button ").append(inputSystem.getTriggerButtonIndex())
                .append(" -> ").append(inputSystem.getTriggerButtonAction()).append("\n");
        text.append(" - Boost button: Button ").append(inputSystem.getBoostButtonIndex()).append("\n");
        text.append(" - Debug mode: ").append(inputSystem.isDebugModeEnabled() ? "ON" : "OFF").append("\n");
        text.append(" - Plane mode: ").append(inputSystem.isSolidPlaneEnabled() ? "Solid retro fill + wireframe" : "Wireframe").append("\n");

        Map<Integer, String> manualMap = inputSystem.getManualMappingForController(snapshot.controllerName());
        text.append("\nManual button mapping (source of truth when present):\n");
        if (manualMap.isEmpty()) {
            text.append(" - none for active controller\n");
        } else {
            for (Map.Entry<Integer, String> entry : manualMap.entrySet()) {
                text.append(" - Button ").append(entry.getKey()).append(" <- ").append(entry.getValue()).append("\n");
            }
        }

        text.append("\nAxis meaning notes (raw names vary by OS/backend):\n");
        text.append(" - Y is usually stick forward/back -> commonly pitch.\n");
        text.append(" - X is usually stick left/right -> commonly yaw or roll.\n");
        text.append(" - RZ is commonly stick twist -> commonly yaw on flight sticks like T.16000M.\n");
        text.append(" - Z/Slider are often throttle controls.\n");
        text.append(" - Exact names can vary by OS/backend; check Raw axes in HUD for live labels.\n");

        text.append("\nConfig:\n");
        text.append(" - Path: ").append(configManager.getConfigPath()).append("\n");

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
        area.setRows(30);
        area.setColumns(72);

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
            shipPanel.updateInputDebug(
                    inputSystem.getPreferredInputDevice(),
                    inputSystem.isKeyboardActive(),
                    inputSystem.getLastJoystickSnapshot(),
                    inputSystem.getActiveInputDescription(),
                    inputSystem
            );
            shipPanel.repaint();
        });

        timer.setCoalesce(true);
        timer.start();
    }
}
