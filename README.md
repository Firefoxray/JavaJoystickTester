# Java Joystick Tester (0.6)

A Swing/Java2D flight-input sandbox for testing keyboard and joystick controls against a ship HUD.

## Version
Current version: `0.6`

## What works in this version
- Main entry point remains `com.fire.javajoysticktester.Main`.
- Single-module Gradle application project (no sample `:app` module).
- Keyboard fallback controls remain active and unchanged.
- Joystick detection/polling via **JInput** with preferred-device + manual-controller selection.
- Joystick axis mapping (pitch/yaw/roll/throttle), trigger action mapping, axis inversion toggles, and clearer flight-meaning labels.
- **Reliable fast button remapping** (`Settings -> Joystick Mapping -> Fast Remap All Buttons...`) with release-aware next-press capture.
- **Quick trigger remap** (`Listen and Set Trigger Button...`) that resolves through manual mappings when present.
- **Persistent config file** auto-load/save at `~/.java-joystick-tester/config.properties` including visual toggles.
- **Reset to defaults** (`Settings -> Reset to Defaults...`) with confirmation and config wipe/rewrite.
- HUD + side panel showing live axis values, controller status, speed (MPH), and compact logical button states.
- Motion-reactive starfield with improved diagonal direction behavior and stronger boost streaking.
- `BOOST` now adds **+500 MPH** while held, with a clear HUD flag and visual speed-up effect.
- `Settings -> Debug Mode` toggle with HUD debug flag, wireframe-default behavior in debug, and front/back ship labels in debug mode.
- Normal mode defaults to solid retro fill + wireframe for readability.
- FIRE_PRIMARY red projectile/light effect now originates from the ship nose/front while held.
- `Extras` menu includes update checks and desktop launcher installation.

## Run options
- Gradle: `./gradlew run`
- Launcher script: `./run_joystick_tester.sh`

## Menu overview
### Settings
- `Preferred Input`: `Auto`, `Keyboard`, `Joystick`
- `Joystick Controller`: auto-select (prefer T.16000M) or manually pin a detected device
- `Joystick Mapping`
  - Pitch/Yaw/Roll/Throttle mapping with explicit flight meanings
  - `Invert Axes` submenu
  - Trigger button selection and trigger action selection
  - `Listen and Set Trigger Button...` (instant next-button capture)
  - `Fast Remap All Buttons...` (full auto-listen mapping pass)
  - `Clear Manual Button Mapping`
- `Debug Mode` (wireframe-default + debug overlays)
- `Controls & Input Status...`
- `Reset to Defaults...` clears in-memory settings and saved config values after confirmation

### View
- `Enable Solid Plane (retro fill)` toggle (wireframe remains available)

### Extras
- `Check for Updates...`
- `Install Desktop Launcher`

## Flight-control meanings (plain language)
- **Pitch** = nose up/down
- **Yaw** = nose left/right
- **Roll** = banking left/right
- **Throttle** = speed control (HUD shows a game-style 100 to 1000 MPH base range)
- **Boost** = temporary +500 MPH overlay while held

## Config persistence
Settings are saved to:
- `~/.java-joystick-tester/config.properties`

Persisted values include preferred input mode, selected controller, axis mappings, invert toggles,
trigger mapping/action, manual button mapping data, debug mode, and visual toggles.

## Joystick support dependencies
- `implementation("net.java.jinput:jinput:2.0.10")`
- `runtimeOnly("net.java.jinput:jinput:2.0.10:natives-all")`

The Gradle `run` task extracts JInput natives into `build/jinput-natives` and sets `java.library.path` accordingly.

## Project layout
- `src/main/java/com/fire/javajoysticktester/Main.java` – app entry point
- `src/main/java/com/fire/javajoysticktester/ui/...` – frame/menu/update loop/config/update flow
- `src/main/java/com/fire/javajoysticktester/render/...` – HUD, ship, starfield, button panel rendering
- `src/main/java/com/fire/javajoysticktester/model/...` – mutable ship model/state
- `src/main/java/com/fire/javajoysticktester/input/...` – keyboard, joystick, and shared input system

See `CHANGELOG.md` for detailed per-change history.
