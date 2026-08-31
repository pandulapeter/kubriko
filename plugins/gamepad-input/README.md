<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Gamepad Input Plugin

The Gamepad Input plugin provides a way to handle game controller events. It reports the analog sticks, the
triggers and the buttons of up to four gamepads through the same layout on every platform, so a game only has to
be written once.

## Features

- **One layout everywhere**: Android, Desktop, iOS and Web all report the same `GamepadButton` values and the
  same stick ranges, with the vertical axes pointing downwards like everywhere else in the engine.
- **Analog input**: sticks and triggers arrive as floats, with a radial dead zone already applied.
- **Actor Integration**: add the `GamepadInputAware` trait to any actor to receive gamepad event callbacks.
- **Hot plugging**: gamepads can be connected and disconnected while the game is running, and slots are sticky,
  so player one doesn't change controller when player two leaves.

## Usage

### 1. Register the Manager

Add the `GamepadInputManager` to your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    GamepadInputManager.newInstance(),
    // ... other managers
)
```

### 2. Use the GamepadInputAware Trait

Implement the `GamepadInputAware` interface in your actor:

```kotlin
class PlayerActor : Actor, GamepadInputAware {

    override fun onGamepadButtonPressed(gamepad: GamepadState, button: GamepadButton) {
        if (button == GamepadButton.SOUTH) {
            // Jump!
        }
    }

    override fun handleGamepadState(gamepad: GamepadState) {
        // Called once per frame for every connected gamepad
        move(gamepad.leftStickX, gamepad.leftStickY, speed = gamepad.leftStickMagnitude)
    }
}
```

### 3. Accessing Gamepad State Manually

You can also query the manager directly from other managers or actors:

```kotlin
val gamepadManager = kubriko.get<GamepadInputManager>()
if (gamepadManager.isButtonPressed(gamepadIndex = 0, button = GamepadButton.START)) {
    // ...
}
```

The `GamepadState` instances in `GamepadInputManager.gamepads` are reused and overwritten on every tick. Read
what you need while you have them and copy anything you want to keep.

## Technical Details

### Platform Backends

| Platform | Backend |
|---|---|
| Android | `MotionEvent` joystick axes and `KEYCODE_BUTTON_*` key events |
| Desktop (JVM) | [Jamepad](https://github.com/libgdx/Jamepad), the libGDX SDL2 binding |
| iOS | The `GameController` framework |
| Web (Wasm) | The [Gamepad API](https://developer.mozilla.org/en-US/docs/Web/API/Gamepad_API) |

### Platform Limitations
- **Web**: browsers hide gamepads until one of their buttons has been pressed, so a controller only shows up
  after the player has used it once. Controllers the browser can't fit into its "standard" mapping report their
  buttons in an order this plugin has no way to interpret.
- **Android**: the plugin consumes the joystick motion events it recognizes, which stops the system from
  synthesizing `KEYCODE_DPAD_*` events out of the left stick for UI focus navigation. Stick input reaches the
  game through this plugin instead of through `keyboard-input`.
- **Desktop**: Jamepad ships the native libraries it needs, which adds a few MB to a packaged distributable. If
  they fail to load, the plugin reports no gamepads rather than failing the game.

## Credits

- [Jamepad](https://github.com/libgdx/Jamepad) - the libGDX fork, Apache 2.0, originally by
  [William Hartman](https://github.com/williamahartman/Jamepad) under the zLib license
- [SDL](https://libsdl.org) - zLib licensed, bundled inside the Jamepad natives

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-gamepad-input`
