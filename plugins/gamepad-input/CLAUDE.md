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
- `src/commonMain/.../GamepadFocusNavigation.kt` — the app-facing half of focus navigation: `GamepadFocusNavigationHost` and the `onGamepadActivation` Modifier with the focus-event node behind it
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

On Android the triggers and the D-pad each arrive from two independent sources - an axis and a key event - which
the handler keeps in separate per-slot arrays and merges (larger value, or bitwise OR). Writing either source
straight into `RawGamepadState` would let the axis path, which runs on every motion event, clear what the key
path set on a device that only has one of the two.

## Focus Navigation

`isFocusNavigationEnabled` makes the gamepad walk **Compose's own focus**, and it is entirely common code — no
platform backend has any part in it, so every target behaves identically. The left stick (reduced to whichever
axis it leans on hardest, Compose having no diagonal to move focus in) and the D-pad call
`FocusManager.moveFocus`, repeating on a delay while a direction is held, the way a held arrow key does.

A directional search only walks the **siblings** of the focused Composable, so a focus parked on a container -
a root that holds it so the game can read keys, which is what most games with a menu look like - has nowhere to
step. A failed step therefore falls back to `FocusDirection.Enter`, which is what Compose does for a D-pad
center, but only while the focus is on nothing the manager knows about: once it is on a declared control,
reaching the end of a list steps out of it rather than dropping into that control's own insides.

The one thing Compose cannot do for us is **activate** the focused control: `clickable` reacts to Enter, Space
and D-pad-center *key events*, and there is no public, portable way to put a key event into the focus system.
So a control declares its own action with `Modifier.onGamepadActivation(manager) { … }`, whose `Modifier.Node`
implements `FocusEventModifierNode` and claims a single slot on the manager while it is the focused one.
`GamepadButton.SOUTH` invokes whatever holds that slot. Everything else — traversal order, geometry, focus
state, focus visuals — stays Compose's.

It runs off the frames of the composition (`Manager.Composable`), not off `onUpdate`, so the menus a game shows
while it is paused stay navigable with its loop stopped. `LocalFocusManager` read there is the **window's**, not
the viewport's, so the focus it moves is the whole UI's.

Gamepad input also claims `InputMode.Keyboard` on the host's `InputModeManager`, the same claim Compose makes for
the arrow keys. Compose decides from the window's input mode both whether a `clickable` is a focus target at all
and whether holding the focus is worth drawing, and a tap or a mouse click puts it into touch mode - without the
claim, a player who touched the screen once would be left steering a focus nothing draws, or none at all.

Which composition the sticks drive is a **stack**, not that one manager: Compose gives a `Popup` or a `Dialog` a
focus system of its own, so content shown in one places a `GamepadFocusNavigationHost` and the innermost host on
screen wins, with the one behind it taking the sticks back on dismissal. The viewport hosts the window's own, so
a game only ever places one for content Compose has moved out of the window - or to claim the host's `onBack`,
which is what `GamepadButton.EAST` runs and the reason a screen with nothing of its own to focus may still want
one. The focused activation targets are a stack for the same reason: a popup's focus system has its own focused
control while the one that opened it goes on holding the focus behind it, so dismissing the popup leaves that
control current again without Compose having to focus it a second time.

Buttons are diffed against the previous tick to produce the discrete pressed/released callbacks. On focus loss
and on disconnection every held button is reported as released and the state is zeroed, so an Actor can't be
left acting on an input that is no longer there.

## Platform Differences

| Platform | Backend | Gotcha |
|---|---|---|
| Android | `OnGenericMotionListener` on the decorView + `OnUnhandledKeyEventListener` | Consuming the joystick motion events suppresses the system's synthetic `KEYCODE_DPAD_*` events for the left stick |
| Desktop (JVM) | Jamepad (SDL2) | One SDL instance per process, reference counted across `Kubriko` instances; a native library that fails to load leaves the plugin inert |
| iOS | `GameController` framework | Vertical axes point upwards there and are negated; controllers without an extended profile are ignored |
| Web (Wasm) | Gamepad API | Gamepads stay invisible until the player presses a button; only the "standard" mapping is interpreted |

## Key API Details

- `handleGamepadState(gamepad)` — called every tick, once per **connected** gamepad; nothing is dispatched for
  empty slots
- `isFocusNavigationEnabled` is meant to be on only while a menu is up; a game reading the same stick would
  otherwise have every push also move the focus around the UI behind it
- `GamepadState` instances are reused - consumers that want to keep values must copy them out
- `gamepads` is a plain `List` rather than a `StateFlow`, because its entries mutate in place; observe
  `connectedGamepadCount` for hot plugging instead
- Sticks use the coordinate system of the engine: positive Y points downwards
- Face buttons are named by position (`SOUTH`/`EAST`/`WEST`/`NORTH`), not by the letters printed on them

## Gotchas

- Do not allocate inside `handleGamepadState` — it runs every tick for every connected gamepad
- Android only: setting the generic motion listener on the decorView replaces any listener already there
- Jamepad ships native libraries for Windows, macOS and Linux, which adds a few MB to a desktop distributable
- Desktop ships `src/desktopMain/resources/kubriko-gamepad-mappings.txt`, and it is not optional: Jamepad loads a
  mapping database during `initSDLGamepad` and prints a caught `IOException` at full stack-trace volume when it
  finds none, its own default path (`/gamecontrollerdb.txt`) naming a file the artifact does not contain. The
  bundled file is deliberately empty of mappings - SDL's compiled-in database stays in charge either way, so the
  only thing it changes is the noise. It sits at the classpath root because that is where Jamepad looks, under
  Kubriko's own name rather than the default one so that a game shipping a real `gamecontrollerdb.txt` keeps it.
  SDL accepts a comment-only database, but never let the file become empty: `SDL_RWFromMem` rejects a zero-length
  buffer, which would put the stack trace back.
