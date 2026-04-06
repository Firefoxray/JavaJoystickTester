# Changelog

## Unreleased
- No unreleased entries at this time.

## 0.4 Alpha

### Added
- Added persistent settings/config support via local properties file at `~/.java-joystick-tester/config.properties`, including graceful fallback to defaults on missing/corrupt config.
- Added full manual button mapping flow (`Settings -> Joystick Mapping -> Map Buttons Manually...`) for step-by-step physical button assignment (`Button 0..N`) and per-controller saved mappings.
- Added `Settings -> Reset to Defaults...` with confirmation dialog to clear in-memory and persisted settings.
- Added axis clarification notes for `Z` vs `RZ` in controls/status text and improved axis option labels for flight-stick usage.

### Changed
- Bumped project/app version references from `0.3 Alpha` to `0.4 Alpha` across `VERSION`, Gradle metadata, README, window title, HUD header, and status text.
- Updated button panel rendering to compact labels (`B0`, `B1`, ...) and tighter layout/font sizing so labels fit cleanly inside cells without overflow.
- Manual button mappings are now treated as source-of-truth for logical button number behavior when configured.

### Fixed
- Fixed long-term starfield drift/collapse toward the bottom-right by updating respawn seeding/randomization so star distribution remains stable over time.

## 0.3 Alpha

### Added
- Added `run_joystick_tester.sh` in the project root as a launcher-friendly script that runs `./gradlew run` from the project directory.
- Added Linux desktop launcher installation flow under `Extras -> Install Desktop Launcher`.
- Added `DesktopLauncherInstaller` to create per-user desktop entry/script/icon assets.
- Added axis inversion toggles for pitch, yaw, roll, and throttle in `Settings -> Joystick Mapping -> Invert Axes`.

### Changed
- Bumped project/app version references from `0.2 Alpha` to `0.3 Alpha` across `VERSION`, Gradle metadata, README, window title, and HUD text.
- Renamed the top-level menu from `Updates` to `Extras`, keeping update checks and adding launcher install.
- Improved starfield effect with higher density and clearer forward-motion streaking while preserving HUD readability and ship visibility.
- Improved button labeling/display by normalizing button keys to numeric `Button N` format when available.
- Expanded trigger button fallback menu range to `Button 0` through `Button 15` when no live button indices are detected.

### Fixed
- Addressed T.16000M button index visibility issues where only a partial `B2-B6` range appeared in some setups by improving identifier parsing and fallback numbering behavior.

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

## 0.1.5

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
