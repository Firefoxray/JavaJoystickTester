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
        addAxisBindingMenu(mappingMenu, "Pitch Axis", inputSystem::getPitchAxis, inputSystem::setPitchAxis);
        addAxisBindingMenu(mappingMenu, "Yaw Axis", inputSystem::getYawAxis, inputSystem::setYawAxis);
        addAxisBindingMenu(mappingMenu, "Roll Axis", inputSystem::getRollAxis, inputSystem::setRollAxis);
        addAxisBindingMenu(mappingMenu, "Throttle Axis", inputSystem::getThrottleAxis, inputSystem::setThrottleAxis);

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

        JMenu triggerActionMenu = new JMenu("Trigger Action");
        ButtonGroup triggerActions = new ButtonGroup();
        for (JoystickButtonAction action : JoystickButtonAction.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(action.name(), action == inputSystem.getTriggerButtonAction());
            item.addActionListener(e -> inputSystem.setTriggerButtonAction(action));
            triggerActions.add(item);
            triggerActionMenu.add(item);
        }
        mappingMenu.add(triggerActionMenu);

        JMenuItem manualMapButtonsItem = new JMenuItem("Map Buttons Manually...");
        manualMapButtonsItem.addActionListener(e -> runManualButtonMapping());
        mappingMenu.addSeparator();
        mappingMenu.add(manualMapButtonsItem);

        JMenuItem clearManualMapItem = new JMenuItem("Clear Manual Button Mapping");
        clearManualMapItem.addActionListener(e -> clearManualButtonMapping());
        mappingMenu.add(clearManualMapItem);

        JMenuItem controlsItem = new JMenuItem("Controls & Input Status...");
        controlsItem.addActionListener(e -> showControlsDialog());

        JMenuItem resetDefaultsItem = new JMenuItem("Reset to Defaults...");
        resetDefaultsItem.addActionListener(e -> resetToDefaults());

        settingsMenu.add(inputMenu);
        settingsMenu.add(joystickMenu);
        settingsMenu.add(mappingMenu);
        settingsMenu.add(controlsItem);
        settingsMenu.addSeparator();
        settingsMenu.add(resetDefaultsItem);
        menuBar.add(settingsMenu);

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

        JoystickSnapshot snapshot = inputSystem.getLastJoystickSnapshot();
        List<Integer> indices = inputSystem.getLogicalButtonIndices(snapshot);
        if (indices.isEmpty()) {
            indices = new ArrayList<>();
            for (int i = 0; i <= 15; i++) {
                indices.add(i);
            }
        }

        for (Integer index : indices) {
            int buttonIndex = index;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem("Button " + buttonIndex, buttonIndex == inputSystem.getTriggerButtonIndex());
            item.addActionListener(e -> inputSystem.setTriggerButtonIndex(buttonIndex));
            group.add(item);
            triggerButtonMenu.add(item);
        }
    }

    private void runManualButtonMapping() {
        JoystickSnapshot snapshot = inputSystem.getLastJoystickSnapshot();
        if (!snapshot.connected()) {
            JOptionPane.showMessageDialog(frame,
                    "Connect/select a joystick first, then open manual mapping.",
                    "Manual Button Mapping",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (snapshot.buttons().isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No digital buttons were reported by the active controller.",
                    "Manual Button Mapping",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int count = snapshot.buttons().size();
        int startChoice = JOptionPane.showConfirmDialog(
                frame,
                "Manual mapping for: " + snapshot.controllerName() + "\n"
                        + "This will prompt from Button 0 to Button " + (count - 1) + ".\n"
                        + "For each step, press+hold the physical button then click OK.",
                "Manual Button Mapping",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (startChoice != JOptionPane.OK_OPTION) {
            return;
        }

        Map<Integer, String> mapped = new LinkedHashMap<>();
        for (int logical = 0; logical < count; logical++) {
            int stepChoice = JOptionPane.showConfirmDialog(
                    frame,
                    "Press and hold physical button for Button " + logical + " and click OK.\n"
                            + "Cancel to finish early.",
                    "Map Button " + logical,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (stepChoice != JOptionPane.OK_OPTION) {
                break;
            }

            String selectedKey = captureButtonKeyForStep(logical);
            if (selectedKey == null) {
                int retry = JOptionPane.showConfirmDialog(
                        frame,
                        "No new pressed button detected for Button " + logical + ". Retry this step?",
                        "Map Button " + logical,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (retry == JOptionPane.YES_OPTION) {
                    logical--;
                }
                continue;
            }

            mapped.put(logical, selectedKey);
        }

        if (mapped.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No button mapping changes were saved.",
                    "Manual Button Mapping",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        inputSystem.setManualMappingForController(snapshot.controllerName(), mapped);
        JOptionPane.showMessageDialog(frame,
                "Saved " + mapped.size() + " button mappings for controller:\n" + snapshot.controllerName(),
                "Manual Button Mapping",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String captureButtonKeyForStep(int logical) {
        List<String> baselinePressed = pressedButtonKeys(inputSystem.pollJoystickSnapshotNow());
        long deadlineMillis = System.currentTimeMillis() + 4000;

        while (System.currentTimeMillis() < deadlineMillis) {
            JoystickSnapshot live = inputSystem.pollJoystickSnapshotNow();
            List<String> pressed = pressedButtonKeys(live);
            List<String> newlyPressed = new ArrayList<>();
            for (String key : pressed) {
                if (!baselinePressed.contains(key)) {
                    newlyPressed.add(key);
                }
            }

            if (!newlyPressed.isEmpty()) {
                if (newlyPressed.size() == 1) {
                    return newlyPressed.getFirst();
                }
                Object selected = JOptionPane.showInputDialog(
                        frame,
                        "Multiple buttons detected for Button " + logical + ". Choose one:",
                        "Map Button " + logical,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        newlyPressed.toArray(),
                        newlyPressed.getFirst()
                );
                return selected == null ? null : selected.toString();
            }

            try {
                Thread.sleep(30L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        return null;
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
                        + "invert flags, manual button maps, and saved config values.",
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
        text.append(" - Pitch axis: ").append(inputSystem.getPitchAxis().label())
                .append(inputSystem.isInvertPitch() ? " (inverted)" : "").append("\n");
        text.append(" - Yaw axis: ").append(inputSystem.getYawAxis().label())
                .append(inputSystem.isInvertYaw() ? " (inverted)" : "").append("\n");
        text.append(" - Roll axis: ").append(inputSystem.getRollAxis().label())
                .append(inputSystem.isInvertRoll() ? " (inverted)" : "").append("\n");
        text.append(" - Throttle axis: ").append(inputSystem.getThrottleAxis().label())
                .append(inputSystem.isInvertThrottle() ? " (inverted)" : "").append("\n");
        text.append(" - Trigger: Button ").append(inputSystem.getTriggerButtonIndex())
                .append(" -> ").append(inputSystem.getTriggerButtonAction()).append("\n");

        Map<Integer, String> manualMap = inputSystem.getManualMappingForController(snapshot.controllerName());
        text.append("\nManual button mapping (source of truth when present):\n");
        if (manualMap.isEmpty()) {
            text.append(" - none for active controller\n");
        } else {
            for (Map.Entry<Integer, String> entry : manualMap.entrySet()) {
                text.append(" - Button ").append(entry.getKey()).append(" <- ").append(entry.getValue()).append("\n");
            }
        }

        text.append("\nAxis meaning notes:\n");
        text.append(" - RZ is commonly joystick twist (yaw) on flight sticks like T.16000M.\n");
        text.append(" - Z is often a throttle/slider axis on many controllers/backends.\n");
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
