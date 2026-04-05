# Changelog

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

### Changed
- Updated app/window header text to `Java Joystick Tester (0.1 Alpha)`.
- Updated Gradle project version to `0.1 Alpha`.
- Updated README to reflect controls, features, run steps, and known limitations.
- Refactored input flow so rendering consumes shared state while input is modularized.

### Fixed
- Fixed keyboard input not being captured reliably by replacing frame-level `KeyListener` usage with Swing key bindings on the render panel (`WHEN_IN_FOCUSED_WINDOW`).
- Ensured update loop applies shared input state before `ShipState.update(...)` and repaint.
- Ensured keyboard fallback remains active when joystick is unavailable.

### Notes
- Main entry point remains `com.fire.javajoysticktester.Main`.
- Swing/Java2D architecture preserved.
