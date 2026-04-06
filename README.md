# Java Joystick Tester (0.5 Alpha)

A Swing/Java2D flight-input sandbox for testing keyboard and joystick controls against a ship HUD.

## Version
Current version: `0.5 Alpha`

## What works in this version
- Main entry point remains `com.fire.javajoysticktester.Main`.
- Single-module Gradle application project (no sample `:app` module).
- Keyboard fallback controls remain active and unchanged.
- Joystick detection/polling via **JInput** with preferred-device + manual-controller selection.
- Joystick axis mapping (pitch/yaw/roll/throttle), trigger action mapping, axis inversion toggles, and clearer flight-meaning labels.
- **Fast button remapping flow** (`Settings -> Joystick Mapping -> Fast Remap All Buttons...`) with automatic next-button capture and cancel/escape support.
- **Quick trigger remap** (`Listen and Set Trigger Button...`) using the same auto-listen flow.
- **Persistent config file** auto-load/save at `~/.java-joystick-tester/config.properties` including visual toggles.
- **Reset to defaults** (`Settings -> Reset to Defaults...`) with confirmation and config wipe/rewrite.
- HUD + side panel showing live axis values, controller status, speed (MPH), and compact button states.
- Motion-reactive starfield (forward, plus pitch/yaw/roll visual response) with respawn safeguards against drift/collapse.
- Optional **solid retro plane mode** (Star Fox SNES-inspired flat fill) while preserving wireframe readability.
- FIRE_PRIMARY red projectile/light effect while the fire button is held.
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
- `Controls & Input Status...`
  - active input/controller
  - mappings and invert state
  - raw axis names vs flight meanings
  - manual button map for active controller
  - config file location
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
- **Throttle** = speed control (HUD shows a game-style 100 to 1000 MPH)

## Fast remapping details
The remapping flow now listens for the **next newly pressed button** and assigns immediately:
1. Open `Settings -> Joystick Mapping -> Fast Remap All Buttons...`
2. For each logical button step (`Button 0..N`), press one physical button.
3. The app auto-detects and stores the mapping.
4. Press `Esc` or click `Cancel` in the listening dialog to stop.

No more “hold button then click OK” requirement.

## Config persistence
Settings are saved to:
- `~/.java-joystick-tester/config.properties`

Persisted values include preferred input mode, selected controller, axis mappings, invert toggles,
trigger mapping/action, manual button mapping data, and visual toggles (including solid plane mode).

If config is missing or invalid, the app safely falls back to defaults.

## Linux joystick permissions (`/dev/input/event*`)
On Linux, JInput commonly reads joystick/gamepad events through `/dev/input/event*` nodes.

If these are unreadable, joystick polling can fail with `Permission denied (13)` even though the app launches normally.
The app surfaces joystick access status in the HUD and in `Controls & Input Status...`.

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
