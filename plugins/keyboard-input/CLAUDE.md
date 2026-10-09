<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-keyboard-input

Provides keyboard event dispatch to Actors and Managers via `KeyboardInputManager` and the `KeyboardInputAware` trait.

## Key Files

- `src/commonMain/.../KeyboardInputManagerImpl.kt` — core state machine; owns the two-buffer key cache
- `src/commonMain/.../KeyboardInputAware.kt` — trait interface for Actors/Managers
- `src/commonMain/.../implementation/KeyboardEventHandler.kt` — the platform key-event source interface and its `expect` factory; `src/*Main/.../implementation/KeyboardEventHandler.*.kt` — one actual per target
- `src/commonMain/.../extensions/KeyExtensions.kt` — convenience helpers on `Key`

## Internal Architecture

Two-buffer design avoids per-frame allocations:
- `activeKeysCache` — mutated on the platform thread when keys change
- `activeKeysSnapshot` — stable copy handed to `handleActiveKeys()` each tick; rebuilt only when the cache is "dirty"

A `hasSentEmptyMap` guard prevents repeated empty-set broadcasts after all keys are released.

`keysPressedSinceLastSnapshot` is a one-tick latch: every press is recorded there as well as in
`activeKeysCache`. When building the per-tick snapshot, latched keys are unioned in, so a key tapped
and released entirely between two ticks (common at low/throttled frame rates, where a tick can span
100 ms) still appears in `handleActiveKeys` for exactly one tick instead of being missed. The latch is
cleared at the end of every `onUpdate`; when it surfaced a now-released key the dirty flag is re-armed
so the following tick rebuilds the snapshot without it. Discrete `onKeyPressed`/`onKeyReleased` fire
off-tick and need no latch; it only serves the polling path. `isKeyPressed` stays strictly
live (no latch) per its contract — use `handleActiveKeys` (or `onKeyPressed`) for tick-accurate taps.

On focus loss, all active keys are flushed immediately, on the main thread like the platform key events, to prevent stuck-key state.

## What Each Backend Hears

- **Desktop**: an AWT listener on the whole toolkit — every key event of every window of the JVM, including keys typed into a Compose `TextField` and the ones Compose uses for focus traversal
- **Web**: `keydown`/`keyup` listeners on `window` — every key event of the page, including keys typed into Compose text fields
- **Android**: `OnUnhandledKeyEventListener` on the decor view — only events no view consumed; Compose consumes arrows and Tab when they move its focus, and a focused `clickable` consumes Enter, Space and D-pad center
- **iOS**: `GCKeyboard` — every hardware key, whatever Compose has focused

## Platform Differences

| Platform | Backend | Gotcha |
|---|---|---|
| Desktop (JVM) | AWT `KeyEvent` | Only left-side modifiers detected (left Shift, left Ctrl, etc.) |
| Web (Wasm) | `KeyboardEvent.code` | `KeyboardEvent.code` mapped with Compose's own table (falling back to `KeyboardEvent.key` for virtual keyboards, as Compose does); right-hand modifiers report the left `Key` (as on Desktop); unmapped keys are ignored. Keys held when the window loses focus are reported as released then, since the browser sends no `keyup` for them |
| Android | `KeyEvent` | 70 ms debounce workaround for unreliable held-key events; the debounce loop runs on the main thread with the key listener |
| iOS | GameController `GCKeyboard` (process-wide handler shared by all instances; it replaces any `keyChangedHandler` the app itself installs on the coalesced keyboard) | Hardware keyboards only (the software keyboard sends no key events); keys are heard whatever Compose has focused, like Desktop and Web. Keys held when the keyboard disconnects are reported as released then |

## Key API Details

- `onKeyPressed(key)` / `onKeyReleased(key)` — fire once per event, NOT on OS key-repeat
- Every `onKeyReleased` follows exactly one `onKeyPressed` of the same instance (or is the focus-loss flush of a held key); a release of a key never reported as pressed is dropped. Presses are focus-gated, so this is what keeps the Desktop listener, which is JVM-wide (it sees every window's keys), from reporting other windows' keys
- `handleActiveKeys(keys: Set<Key>)` — called every tick with the current held set; use for smooth movement
- `isKeyPressed(key)` reads `activeKeysCache` (live), not the per-tick snapshot
- `KeyboardInputAware` can be applied to **Managers** as well as Actors

## `KeyExtensions` Helpers

```kotlin
keys.directionState    // KeyboardDirectionState from WASD/arrow keys
keys.zoomState         // KeyboardZoomState from +/- keys
keys.hasLeft/Right/Up/Down  // Boolean shortcuts
Key.displayName          // Human-readable label
```

## Gotchas

- Keys typed into a text field reach the game on Desktop, Web and iOS; Compose focus traversal and focused clickables can consume arrows, Tab, Enter and Space before the game sees them on Android
- Key repeat from the OS is swallowed; `onKeyPressed` fires exactly once per physical press
- Web: keys missing from Compose's web table (media keys, `PrintScreen`, `Pause`, …) are ignored, not reported as `Key.Unknown`
- Android: the 70 ms debounce means very short key taps may be missed
- Do not allocate inside `handleActiveKeys` — it runs every tick