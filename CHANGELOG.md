# Changelog

## Unreleased

### Changed
- Improved joystick auto-selection to prefer Thrustmaster T.16000M and deprioritize common virtual/non-flight devices in auto mode.
- Added manual controller selection menu (`Settings -> Joystick Controller`) so users can pin a specific detected controller.
- Improved HUD and controls dialog to make active input/controller explicit and list all detected controller names with active marker.
- Kept keyboard fallback behavior unchanged when joystick is unavailable or keyboard mode is chosen.
- Added joystick axis/button mapping options (pitch/yaw/roll/throttle axis selection plus trigger button/action binding).
- Updated joystick steering to direct (Star-Fox-style) target control so stick movement drives ship attitude directly.
- Reworked starfield animation so white star dots stream toward the camera, improving perceived forward ship motion.
- Removed accidental sample multi-module leftovers so Gradle now runs as a single-module project.
- Removed the generated `:app:run` "Hello World!" sample task/module path.
- Added joystick access status text to both HUD and `Controls & Input Status...` dialog.
- Updated README with Linux `/dev/input/event*` permission guidance and Fedora-specific troubleshooting notes.

### Fixed
- Added Linux permission diagnostics for unreadable `/dev/input/event*` nodes and surfaced a friendly in-app hint when joystick access likely fails due to permissions.

## 0.1 Alpha

### Added
- Added `VERSION` file with current project version.
- Added joystick support through JInput dependencies in Gradle.
- Added new input architecture classes:
  - `InputSystem`
  - `JoystickInput`
  - `JoystickSnapshot`
  - `PreferredInputDevice`
- Added joystick detection and controller listing support.
- Added T.16000M name-based detection in joystick status.
- Added in-app settings menu with preferred input selection (`Auto/Keyboard/Joystick`).
- Added `Controls & Input Status...` submenu dialog with bindings and live status.
- Added HUD debug lines for input mode, keyboard active state, joystick status, and raw axis values.
- Added Gradle native-runtime wiring for JInput by introducing `extractJinputNatives` to unpack native libraries into `build/jinput-natives` before launch.

### Changed
- Updated app/window header text to `Java Joystick Tester (0.1 Alpha)`.
- Updated Gradle project version to `0.1 Alpha`.
- Updated README to reflect controls, features, run steps, and known limitations.
- Refactored input flow so rendering consumes shared state while input is modularized.
- Updated runtime launch configuration so the `run` task now passes `-Djava.library.path=<project>/build/jinput-natives`.

### Fixed
- Fixed keyboard input not being captured reliably by replacing frame-level `KeyListener` usage with Swing key bindings on the render panel (`WHEN_IN_FOCUSED_WINDOW`).
- Ensured update loop applies shared input state before `ShipState.update(...)` and repaint.
- Ensured keyboard fallback remains active when joystick is unavailable.
- Fixed Linux joystick runtime failures (`no jinput-linux64 in java.library.path` and `LinuxEnvironmentPlugin is not supported`) by unpacking and exposing JInput natives during Gradle app execution.

### Notes
- Main entry point remains `com.fire.javajoysticktester.Main`.
- Swing/Java2D architecture preserved.
