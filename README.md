# Java Joystick Tester (0.3 Alpha)

A Swing/Java2D flight-input sandbox for testing keyboard and joystick controls against a ship HUD.

## Version
Current version: `0.3 Alpha`

## What works in this version
- Main entry point remains `com.fire.javajoysticktester.Main`.
- Single-module Gradle application project (no sample `:app` module).
- Keyboard fallback controls remain active and unchanged.
- Joystick detection/polling via **JInput** with preferred-device + manual-controller selection.
- Joystick axis mapping (pitch/yaw/roll/throttle), trigger action mapping, and new invert toggles for pitch/yaw/roll/throttle.
- HUD + side panel showing live axis values, controller status, and button states.
- T.16000M button display uses normalized numeric button IDs when available (targeting `Button 0`..`Button 15` layout).
- Improved starfield density and motion streaking for stronger forward-flight feel.
- `Extras` menu includes update checks and desktop launcher installation.

## Run options
- Gradle: `./gradlew run`
- Launcher script (new): `./run_joystick_tester.sh`

The launcher script is suitable as a desktop-entry target and always runs from the project directory.

## Menu overview
### Settings
- `Preferred Input`: `Auto`, `Keyboard`, `Joystick`
- `Joystick Controller`: auto-select (prefer T.16000M) or manually pin a detected device
- `Joystick Mapping`
  - Pitch/Yaw/Roll/Throttle axis selection
  - **Invert Axes**: invert Pitch, Yaw, Roll, Throttle
  - Trigger button selection (from detected button indices)
  - Trigger action (`NONE`, `BOOST`, `FIRE_PRIMARY`)
- `Controls & Input Status...`: shows active mode, mappings, inversion state, and controller list

### Extras
- `Check for Updates...`
- `Install Desktop Launcher`

## Desktop launcher installation
Using `Extras -> Install Desktop Launcher` on Linux creates:
- `~/.local/share/applications/java-joystick-tester.desktop`
- `~/.local/share/java-joystick-tester/run_joystick_tester.sh`
- `~/.local/share/icons/java-joystick-tester/icon1.png`

The launcher uses the included `images/icon1.png` and starts the app through Gradle.

## T.16000M button numbering note
JInput sometimes reports generic/raw component names depending on platform/backend.
This build normalizes button labels using numeric identifiers where available and falls back to component order when not.

- On typical T.16000M setups, this should show all 16 buttons (`Button 0`..`Button 15`).
- If your OS/backend exposes non-numeric names only, fallback ordering may still need one-time manual trigger-button selection in the menu.

## Linux joystick permissions (`/dev/input/event*`)
On Linux, JInput commonly reads joystick/gamepad events through `/dev/input/event*` nodes.

If these are unreadable, joystick polling can fail with `Permission denied (13)` even though the app launches normally.

The app surfaces joystick access status in the HUD and in `Controls & Input Status...`.

## Joystick support dependencies
- `implementation("net.java.jinput:jinput:2.0.10")`
- `runtimeOnly("net.java.jinput:jinput:2.0.10:natives-all")`

The Gradle `run` task extracts JInput natives into `build/jinput-natives` and sets `java.library.path` accordingly.

## Limitations (0.3 Alpha)
- No persistent remapping profile save/load yet.
- No hot-plug notification UI yet (controllers are polled each frame).
- Exact physical-button-to-index mapping can vary by OS/JInput backend even when numeric labels are available.

## Project layout
- `src/main/java/com/fire/javajoysticktester/Main.java` – app entry point
- `src/main/java/com/fire/javajoysticktester/ui/...` – frame/menu/update loop + launcher install
- `src/main/java/com/fire/javajoysticktester/render/...` – HUD, ship, and starfield rendering
- `src/main/java/com/fire/javajoysticktester/model/...` – mutable ship model/state
- `src/main/java/com/fire/javajoysticktester/input/...` – keyboard, joystick, and shared input system

See `CHANGELOG.md` for detailed per-change history.
