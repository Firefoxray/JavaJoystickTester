# Changelog

## Unreleased

## 0.2 Alpha

### Added
- Added in-app update menu: `Updates -> Check for Updates...`.
- Added `GitUpdateService` to encapsulate Git-based update checks and update execution with restart attempt via `ProcessBuilder`.

### Changed
- Bumped project/app version from `0.1 Alpha` to `0.2 Alpha` across `VERSION`, Gradle metadata, README, and in-app header strings.
- Update dialogs now show current branch, short commit hash, and clear status outcomes (`up to date`, `update available`, `unable to check`, `working tree dirty`).
- Update flow now prompts for confirmation before update; choosing `Yes` performs `git pull --ff-only` on the current branch and attempts app restart, while choosing `No` returns to the app.
- Added robust update checks for dirty working tree, detached HEAD, missing upstream, non-Git execution context, fetch failures, and other Git command errors.
- Added asynchronous menu-driven update check/update calls so UI remains responsive while Git commands run.
- Added/retained visual HUD improvements from the previous pass: denser starfield, clearer forward-facing ship nose/cockpit cues, and side-panel live button states by index for active controller.

### Fixed
- Auto-update is now explicitly blocked when local uncommitted changes are present, with a friendly in-app message.

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
