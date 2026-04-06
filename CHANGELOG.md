# Changelog

## Unreleased
- No unreleased entries at this time.

## 0.8

### Changed
- Bumped project/app version references from `0.7` to `0.8` across `VERSION`, Gradle metadata, README, and shared app constants.
- Flipped ship render orientation back so the ship front points away from the viewer and the back faces the camera.
- Rebalanced starfield movement to significantly reduce disorienting motion at low speed, with much gentler yaw/pitch/roll drift (including diagonal movement) and a lower base forward-scroll rate.

## 0.7

### Added
- Added per-button remap UX for all 16 logical joystick buttons via `Settings -> Joystick Mapping -> Remap Logical Buttons`, where each logical button is remapped one-at-a-time from the next physical press only.
- Added dedicated boost button mapping controls (`Boost Button` selector + `Remap Boost Button...`) with default logical mapping set to `Button 1`.

### Changed
- Bumped project/app version references from `0.6` to `0.7` across `VERSION`, Gradle metadata, README, window title, HUD text, and shared app constants.
- Rebalanced starfield movement so forward motion remains strong while yaw/pitch/roll turn-parallax is toned down and less exaggerated.
- Replaced the broken full-sequence "remap all" flow with responsive per-button remap actions to avoid UI lock-ups.
- Updated logical button presentation to always provide a stable `B0..B15` logical set for mapping/selection workflows.
- Flipped ship render orientation so the craft visually flies away from the camera (back facing viewer).

### Fixed
- Fixed duplicate-looking logical button labels (`B0, B0` / `B1, 1`-style confusion) by normalizing displayed labels and only showing mapping arrows when logical/physical indices differ.
- Kept FIRE_PRIMARY/projectile origin on the ship front/nose after orientation changes.
- Preserved manual button mapping as source-of-truth for trigger/boost logical button behavior when mappings are present.

## 0.6

### Added
- Added `Settings -> Debug Mode` toggle with a visible HUD `DEBUG MODE` indicator.
- Added debug-only ship orientation labels (`FRONT`/`BACK`) to make nose/tail direction obvious while tuning controls.

### Changed
- Bumped project/app version references from `0.5 Alpha` to `0.6` across `VERSION`, Gradle metadata, README, window title, HUD text, and shared app constants.
- Updated BOOST behavior so holding BOOST adds a temporary **+500 MPH** to current speed while preserving the normal MPH readout baseline.
- Improved boost visuals with faster/longer star streaking and brighter boost-era star tinting.
- Updated debug mode default rendering behavior: debug ON defaults to wireframe; debug OFF defaults to solid retro fill + wireframe.

### Fixed
- Fixed diagonal starfield direction issues by switching directional star shifts to stable per-frame attitude deltas (yaw/pitch/roll), preventing wrong-way diagonal movement while keeping forward-flight feel.
- Fixed FIRE_PRIMARY origin to consistently emit from the ship nose/front rather than the back.
- Improved button remap reliability by waiting for release and then detecting newly pressed buttons from rolling snapshots (reducing stale-snapshot misses).
- Fixed trigger remap behavior to honor manual mapping as source-of-truth when present, so displayed logical labels and action behavior stay aligned (including T.16000M workflows).

## 0.5 Alpha

### Added
- Added user-friendly flight control language across menus/HUD/status text (Pitch = nose up/down, Yaw = nose left/right, Roll = bank left/right), including clearer raw-axis labels for mapping.
- Added instant-listen button remap UX for full controller mapping (`Fast Remap All Buttons...`) that auto-captures the next pressed button with Cancel/Escape support.
- Added quick trigger remap action (`Listen and Set Trigger Button...`) using the same next-press listening flow.
- Added optional `View -> Enable Solid Plane (retro fill)` mode with flat filled polygons inspired by retro Star Fox-style visuals while preserving wireframe outlines.
- Added HUD speed readout in MPH derived from throttle (`0% ~= 100 MPH`, `100% ~= 1000 MPH`).
- Added FIRE_PRIMARY hold visual: red projectile/glow effect in front of the ship while the fire button is pressed.

### Changed
- Bumped project/app version references from `0.4 Alpha` to `0.5 Alpha` across `VERSION`, Gradle metadata, README, window title, HUD text, and shared app constants.
- Updated starfield behavior so pitch/yaw/roll influence perceived star motion (up/down and left/right response) while preserving forward-flight feel.
- Expanded settings persistence to include visual toggles (such as solid plane mode).
- Reset-to-defaults flow now explicitly includes visual toggles along with mappings and input settings.

### Fixed
- Kept star respawn safeguards in place while adding motion-reactive drift so the long-term drift/collapse bug is not reintroduced.

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
