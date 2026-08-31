<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-gamepad-input

Provides gamepad event dispatch to Actors and Managers via `GamepadInputManager` and the `GamepadInputAware`
trait, in a single layout shared by all four platforms.

## Key Files

- `src/commonMain/.../GamepadInputManagerImpl.kt` — polls the platform handler once per tick, applies the dead
  zone, diffs the buttons and dispatches to the Actors
- `src/commonMain/.../GamepadState.kt` — the public, reused per-slot state object
- `src/commonMain/.../GamepadButton.kt` — the shared button layout; `bitMask` is the ordinal-derived bit
- `src/commonMain/.../implementation/RawGamepadState.kt` — what the platform handlers write into
- `src/*/kotlin/.../GamepadEventHandler.*.kt` — platform-specific backend (one per target)

## Internal Architecture

Two arrays of `MAX_GAMEPAD_COUNT` entries, both allocated once and mutated in place:
- `rawGamepads` — filled by the platform handler with values exactly as the platform reports them
- `gamepads` — the public `GamepadState` objects, derived from the raw ones each tick

Slots are sticky in every backend: a controller keeps the index it was first given until it disconnects.

The manager, not the platform code, applies the radial dead zone and derives the digital `LEFT_TRIGGER` /
`RIGHT_TRIGGER` buttons from the analog values. Triggers are the one input every platform reports differently
(axis, button, or both), so normalising them in one place is what keeps the layout identical everywhere.

Buttons are diffed against the previous tick to produce the discrete pressed/released callbacks. On focus loss
and on disconnection every held button is reported as released and the state is zeroed, so an Actor can't be
left acting on an input that is no longer there.

## Platform Differences

| Platform | Backend | Gotcha |
|---|---|---|
| Android | `OnGenericMotionListener` on the decorView + `OnUnhandledKeyEventListener` | The D-pad is read from the hat axes, because `keyboard-input` may consume the `KEYCODE_DPAD_*` events first |
| Desktop (JVM) | Jamepad (SDL2) | One SDL instance per process, reference counted across `Kubriko` instances; a native library that fails to load leaves the plugin inert |
| iOS | `GameController` framework | Vertical axes point upwards there and are negated; controllers without an extended profile are ignored |
| Web (Wasm) | Gamepad API | Gamepads stay invisible until the player presses a button; only the "standard" mapping is interpreted |

## Key API Details

- `handleGamepadState(gamepad)` — called every tick, once per **connected** gamepad; nothing is dispatched for
  empty slots
- `GamepadState` instances are reused - consumers that want to keep values must copy them out
- `gamepads` is a plain `List` rather than a `StateFlow`, because its entries mutate in place; observe
  `connectedGamepadCount` for hot plugging instead
- Sticks use the coordinate system of the engine: positive Y points downwards
- Face buttons are named by position (`SOUTH`/`EAST`/`WEST`/`NORTH`), not by the letters printed on them

## Gotchas

- Do not allocate inside `handleGamepadState` — it runs every tick for every connected gamepad
- Android only: setting the generic motion listener on the decorView replaces any listener already there
- Jamepad ships native libraries for Windows, macOS and Linux, which adds a few MB to a desktop distributable
