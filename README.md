# Java Joystick Tester (0.4 Alpha)

A Swing/Java2D flight-input sandbox for testing keyboard and joystick controls against a ship HUD.

## Version
Current version: `0.4 Alpha`

## What works in this version
- Main entry point remains `com.fire.javajoysticktester.Main`.
- Single-module Gradle application project (no sample `:app` module).
- Keyboard fallback controls remain active and unchanged.
- Joystick detection/polling via **JInput** with preferred-device + manual-controller selection.
- Joystick axis mapping (pitch/yaw/roll/throttle), trigger action mapping, and axis inversion toggles.
- **Manual full button mapping flow** (`Settings -> Joystick Mapping -> Map Buttons Manually...`) that walks `Button 0..N` and saves per-controller physical mappings.
- **Reset to defaults** (`Settings -> Reset to Defaults...`) with confirmation and config wipe.
- **Persistent config file** auto-load/save at `~/.java-joystick-tester/config.properties`.
- HUD + side panel showing live axis values, controller status, and compact button states.
- Starfield forward-flight background with respawn logic fixed to avoid long-term bottom-right drift/collapse.
- `Extras` menu includes update checks and desktop launcher installation.

## Run options
- Gradle: `./gradlew run`
- Launcher script: `./run_joystick_tester.sh`

## Menu overview
### Settings
- `Preferred Input`: `Auto`, `Keyboard`, `Joystick`
- `Joystick Controller`: auto-select (prefer T.16000M) or manually pin a detected device
- `Joystick Mapping`
  - Pitch/Yaw/Roll/Throttle axis selection
  - `Invert Axes` submenu
  - Trigger button selection and trigger action selection
  - `Map Buttons Manually...` (step-by-step physical button capture)
  - `Clear Manual Button Mapping`
- `Controls & Input Status...`
  - active input/controller
  - mappings and invert state
  - manual button map for active controller
  - axis meaning notes (`Z` vs `RZ`)
  - config file location
- `Reset to Defaults...` clears in-memory settings and saved config values after confirmation

### Extras
- `Check for Updates...`
- `Install Desktop Launcher`

## Manual button mapping details
Manual mapping is intended as the source of truth when device-reported button identifiers are unreliable.

Flow:
1. Open `Settings -> Joystick Mapping -> Map Buttons Manually...`
2. The app determines the active controller's button count.
3. It prompts through logical `Button 0`, `Button 1`, ... up to `Button N`.
4. For each prompt, hold the desired physical button and click OK.
5. Mapping is saved per controller name and reused on restart.

This is especially useful for T.16000M layouts where backend button identifiers may vary by OS.

## Z vs RZ axis note
JInput axis names vary by platform/backend, but this app now labels options with likely flight-stick meaning:
- `RZ` is commonly stick twist/yaw on devices like the T.16000M.
- `Z` is commonly throttle/slider on many devices.

Always verify with live `Raw axes` HUD readout, since exact labeling can differ.

## Config persistence
Settings are saved to:
- `~/.java-joystick-tester/config.properties`

Persisted values include preferred input mode, selected controller, axis mappings, invert toggles,
trigger mapping/action, and manual button mapping data.

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
