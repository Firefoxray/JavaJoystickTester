package com.fire.javajoysticktester.ui;

import com.fire.javajoysticktester.input.InputSystem;
import com.fire.javajoysticktester.input.JoystickAxisOption;
import com.fire.javajoysticktester.input.JoystickButtonAction;
import com.fire.javajoysticktester.input.PreferredInputDevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Persists user settings in a local editable properties file.
 */
public class AppConfigManager {
    private static final String CONFIG_DIR = ".java-joystick-tester";
    private static final String CONFIG_FILE = "config.properties";

    private final Path configPath;

    public AppConfigManager() {
        Path home = Path.of(System.getProperty("user.home", "."));
        this.configPath = home.resolve(CONFIG_DIR).resolve(CONFIG_FILE);
    }

    public Path getConfigPath() {
        return configPath;
    }

    public void loadInto(InputSystem inputSystem) {
        ensureConfigExists();

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (Exception ex) {
            inputSystem.resetToDefaults();
            return;
        }

        try {
            inputSystem.runWithoutSettingsNotifications(() -> {
                inputSystem.resetToDefaults();
                inputSystem.setPreferredInputDevice(parseEnum(properties.getProperty("preferred_input"), PreferredInputDevice.class, PreferredInputDevice.AUTO));
                inputSystem.setSelectedJoystickName(blankToNull(properties.getProperty("selected_controller")));

                inputSystem.setPitchAxis(parseEnum(properties.getProperty("axis.pitch"), JoystickAxisOption.class, JoystickAxisOption.Y));
                inputSystem.setYawAxis(parseEnum(properties.getProperty("axis.yaw"), JoystickAxisOption.class, JoystickAxisOption.X));
                inputSystem.setRollAxis(parseEnum(properties.getProperty("axis.roll"), JoystickAxisOption.class, JoystickAxisOption.RZ));
                inputSystem.setThrottleAxis(parseEnum(properties.getProperty("axis.throttle"), JoystickAxisOption.class, JoystickAxisOption.SLIDER));

                inputSystem.setInvertPitch(Boolean.parseBoolean(properties.getProperty("invert.pitch", "false")));
                inputSystem.setInvertYaw(Boolean.parseBoolean(properties.getProperty("invert.yaw", "false")));
                inputSystem.setInvertRoll(Boolean.parseBoolean(properties.getProperty("invert.roll", "false")));
                inputSystem.setInvertThrottle(Boolean.parseBoolean(properties.getProperty("invert.throttle", "false")));

                inputSystem.setTriggerButtonIndex(parseInt(properties.getProperty("trigger.button"), 0));
                inputSystem.setTriggerButtonAction(parseEnum(properties.getProperty("trigger.action"), JoystickButtonAction.class, JoystickButtonAction.FIRE_PRIMARY));
                inputSystem.setSolidPlaneEnabled(Boolean.parseBoolean(properties.getProperty("visual.solid_plane", "true")));
                inputSystem.setDebugModeEnabled(Boolean.parseBoolean(properties.getProperty("visual.debug_mode", "false")));

                inputSystem.setManualButtonMappings(parseManualMappings(properties));
            });
            saveFrom(inputSystem);
        } catch (Exception ex) {
            inputSystem.resetToDefaults();
        }
    }

    public void saveFrom(InputSystem inputSystem) {
        ensureConfigExists();

        Properties properties = new Properties();
        properties.setProperty("preferred_input", inputSystem.getPreferredInputDevice().name());
        properties.setProperty("selected_controller", nullToBlank(inputSystem.getSelectedJoystickName()));

        properties.setProperty("axis.pitch", inputSystem.getPitchAxis().name());
        properties.setProperty("axis.yaw", inputSystem.getYawAxis().name());
        properties.setProperty("axis.roll", inputSystem.getRollAxis().name());
        properties.setProperty("axis.throttle", inputSystem.getThrottleAxis().name());

        properties.setProperty("invert.pitch", Boolean.toString(inputSystem.isInvertPitch()));
        properties.setProperty("invert.yaw", Boolean.toString(inputSystem.isInvertYaw()));
        properties.setProperty("invert.roll", Boolean.toString(inputSystem.isInvertRoll()));
        properties.setProperty("invert.throttle", Boolean.toString(inputSystem.isInvertThrottle()));

        properties.setProperty("trigger.button", Integer.toString(inputSystem.getTriggerButtonIndex()));
        properties.setProperty("trigger.action", inputSystem.getTriggerButtonAction().name());
        properties.setProperty("visual.solid_plane", Boolean.toString(inputSystem.isSolidPlaneEnabled()));
        properties.setProperty("visual.debug_mode", Boolean.toString(inputSystem.isDebugModeEnabled()));

        for (Map.Entry<String, Map<Integer, String>> controllerEntry : inputSystem.getManualButtonMappings().entrySet()) {
            String controller = sanitize(controllerEntry.getKey());
            for (Map.Entry<Integer, String> mappingEntry : controllerEntry.getValue().entrySet()) {
                String key = "button_map." + controller + "." + mappingEntry.getKey();
                properties.setProperty(key, mappingEntry.getValue());
            }
        }

        try (OutputStream out = Files.newOutputStream(configPath)) {
            properties.store(out, "Java Joystick Tester user config");
        } catch (IOException ex) {
            // ignore save failures to keep UI responsive
        }
    }

    public void resetConfigFile() {
        try {
            Files.deleteIfExists(configPath);
        } catch (IOException ex) {
            // ignore
        }
    }

    private void ensureConfigExists() {
        try {
            Files.createDirectories(configPath.getParent());
            if (!Files.exists(configPath)) {
                Files.createFile(configPath);
            }
        } catch (IOException ex) {
            // ignore
        }
    }

    private static String sanitize(String name) {
        return name.replace("\\", "\\\\").replace(".", "\\.");
    }

    private static String unsanitize(String name) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (char c : name.toCharArray()) {
            if (escaped) {
                out.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static Map<String, Map<Integer, String>> parseManualMappings(Properties properties) {
        Map<String, Map<Integer, String>> mappings = new HashMap<>();

        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("button_map.")) {
                continue;
            }

            String payload = key.substring("button_map.".length());
            int split = findLastUnescapedDot(payload);
            if (split <= 0 || split >= payload.length() - 1) {
                continue;
            }

            String controller = unsanitize(payload.substring(0, split));
            int logicalIndex = parseInt(payload.substring(split + 1), -1);
            if (logicalIndex < 0) {
                continue;
            }

            mappings.computeIfAbsent(controller, ignored -> new LinkedHashMap<>())
                    .put(logicalIndex, properties.getProperty(key, ""));
        }

        return mappings;
    }

    private static int findLastUnescapedDot(String text) {
        boolean escaped = false;
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '.') {
                return i;
            }
        }
        return -1;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static <T extends Enum<T>> T parseEnum(String raw, Class<T> type, T fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
