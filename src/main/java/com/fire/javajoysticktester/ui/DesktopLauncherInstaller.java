package com.fire.javajoysticktester.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Installs a Linux desktop launcher for the current user.
 */
public final class DesktopLauncherInstaller {
    private static final String DESKTOP_FILE_NAME = "java-joystick-tester.desktop";
    private static final String ICON_FILE_NAME = "icon1.png";
    private static final String APP_DIR_NAME = "java-joystick-tester";

    private DesktopLauncherInstaller() {
    }

    public static InstallResult installForCurrentUser(Path projectRoot) {
        try {
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (!osName.contains("linux")) {
                return InstallResult.failure("Desktop launcher installation is currently supported on Linux only.");
            }

            Path home = Path.of(System.getProperty("user.home"));
            Path applicationsDir = home.resolve(".local/share/applications");
            Path iconsDir = home.resolve(".local/share/icons").resolve(APP_DIR_NAME);
            Path appDataDir = home.resolve(".local/share").resolve(APP_DIR_NAME);
            Path desktopFilePath = applicationsDir.resolve(DESKTOP_FILE_NAME);
            Path iconSource = projectRoot.resolve("images").resolve(ICON_FILE_NAME);
            Path iconTarget = iconsDir.resolve(ICON_FILE_NAME);
            Path launcherScript = appDataDir.resolve("run_joystick_tester.sh");

            if (!Files.exists(iconSource)) {
                return InstallResult.failure("Cannot find icon image at: " + iconSource);
            }

            Files.createDirectories(applicationsDir);
            Files.createDirectories(iconsDir);
            Files.createDirectories(appDataDir);

            Files.copy(iconSource, iconTarget, StandardCopyOption.REPLACE_EXISTING);

            String launcherScriptText = "#!/usr/bin/env bash\n"
                    + "set -euo pipefail\n"
                    + "cd \"" + escapeForShell(projectRoot.toAbsolutePath().toString()) + "\"\n"
                    + "exec ./gradlew run\n";

            Files.writeString(launcherScript, launcherScriptText, StandardCharsets.UTF_8);
            setExecutable(launcherScript);

            String desktopFileContent = "[Desktop Entry]\n"
                    + "Type=Application\n"
                    + "Version=1.0\n"
                    + "Name=Java Joystick Tester\n"
                    + "Comment=Swing/Java2D joystick tester\n"
                    + "Exec=" + launcherScript.toAbsolutePath() + "\n"
                    + "Icon=" + iconTarget.toAbsolutePath() + "\n"
                    + "Terminal=false\n"
                    + "Categories=Game;Utility;\n"
                    + "StartupNotify=true\n";

            Files.writeString(desktopFilePath, desktopFileContent, StandardCharsets.UTF_8);

            return InstallResult.success(
                    "Desktop launcher installed:\n"
                            + "- " + desktopFilePath + "\n"
                            + "- " + launcherScript + "\n"
                            + "- " + iconTarget
            );
        } catch (Exception ex) {
            return InstallResult.failure("Failed to install desktop launcher: " + ex.getMessage());
        }
    }

    private static String escapeForShell(String path) {
        return path.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void setExecutable(Path launcherScript) throws IOException {
        try {
            Set<PosixFilePermission> permissions = Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE
            );
            Files.setPosixFilePermissions(launcherScript, permissions);
        } catch (UnsupportedOperationException ignored) {
            launcherScript.toFile().setExecutable(true, false);
        }
    }

    public record InstallResult(boolean success, String message) {
        public static InstallResult success(String message) {
            return new InstallResult(true, message);
        }

        public static InstallResult failure(String message) {
            return new InstallResult(false, message);
        }
    }
}
