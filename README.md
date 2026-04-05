# Java Joystick Tester (0.1 Alpha)

A Swing/Java2D flight-input sandbox for testing keyboard and joystick controls against a simple ship debug HUD.

## Version
Current version: `0.1 Alpha`

## What works in this version
- Main entry point remains `com.fire.javajoysticktester.Main`.
- Fixed keyboard input handling by using Swing key bindings (`WHEN_IN_FOCUSED_WINDOW`) instead of frame-level `KeyListener`.
- Real-time ship HUD for pitch, yaw, roll, and throttle.
- Input architecture split into keyboard input, joystick input, and shared input system.
- Joystick detection and polling via **JInput**.
- On-screen joystick status (connected device name, T.16000M detection, raw axis values).
- Settings menu for preferred input device selection and controls/status view.

## Controls
Keyboard (fallback and explicit mode):
- `Up/Down`: Pitch
- `Left/Right`: Yaw
- `Q/E`: Roll
- `W/S`: Throttle

Menu:
- `Settings -> Preferred Input`: `Auto`, `Keyboard`, `Joystick`
- `Settings -> Controls & Input Status...`: Shows bindings, active status, detected controllers

## Joystick support
Dependencies:
- `implementation("net.java.jinput:jinput:2.0.10")`
- `runtimeOnly("net.java.jinput:jinput:2.0.10:natives-all")`

Runtime native setup (Gradle):
- `extractJinputNatives` unpacks native files from the `natives-all` runtime artifact into `build/jinput-natives`.
- The Gradle `run` task depends on that extraction task.
- The Gradle `run` task sets `-Djava.library.path=<project>/build/jinput-natives` before launching `Main`.

Why this matters:
- Java classes from `jinput` can load fine without natives, so the app window can still launch.
- Joystick backends (like `LinuxEnvironmentPlugin`) require platform native libraries at runtime.
- If `java.library.path` does not include unpacked JInput natives, joystick discovery fails with errors like `no jinput-linux64 in java.library.path`.

## Run from IntelliJ and Gradle
You should keep running from `Main`:
- Class: `com.fire.javajoysticktester.Main`

Recommended execution modes:
- Gradle: `./gradlew run`
- IntelliJ (recommended): enable **Run tests using: Gradle** and **Build and run using: Gradle**, then run the Gradle `run` task.

If you run a plain IntelliJ Application configuration directly (not delegated to Gradle), add this VM option:
- `-Djava.library.path=$ProjectFileDir$/build/jinput-natives`

and run this once first:
```bash
./gradlew extractJinputNatives
```

## Limitations (0.1 Alpha)
- Joystick axis mapping is generic and may vary by hardware.
- No persistent control remapping UI yet.
- No hot-plug notifications yet (controllers are polled each frame).

## Project layout
- `Main.java` – app entry point
- `ui/...` – frame/menu/update loop
- `render/...` – HUD and ship rendering
- `model/...` – mutable ship model/state
- `input/...` – keyboard, joystick, and shared input system

See `CHANGELOG.md` for detailed per-change history.
