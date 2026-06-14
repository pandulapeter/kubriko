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
- `src/*/kotlin/.../KeyboardInputSource.kt` — platform-specific key-event source (one per target)
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
so the following tick rebuilds the snapshot without it. Discrete `onKeyPressed`/`onKeyReleased` already
fire off-tick and were never affected; this only fixes the polling path. `isKeyPressed` stays strictly
live (no latch) per its contract — use `handleActiveKeys` (or `onKeyPressed`) for tick-accurate taps.

On focus loss, all active keys are flushed immediately to prevent stuck-key state.

## Platform Differences

| Platform | Backend | Gotcha |
|---|---|---|
| Desktop (JVM) | AWT `KeyEvent` | Only left-side modifiers detected (left Shift, left Ctrl, etc.) |
| Web (Wasm) | `KeyboardEvent.code` | Many keys return `Key(-1)` (unmapped); test on target |
| Android | `KeyEvent` | 70 ms debounce workaround for unreliable held-key events |
| iOS | Zero-size UIView first-responder | Limited key support; software keyboard only |

## Key API Details

- `onKeyPressed(key)` / `onKeyReleased(key)` — fire once per event, NOT on OS key-repeat
- `handleActiveKeys(keys: Set<Key>)` — called every tick with the current held set; use for smooth movement
- `isKeyPressed(key)` reads `activeKeysCache` (live), not the per-tick snapshot
- `KeyboardInputAware` can be applied to **Managers** as well as Actors

## `KeyExtensions` Helpers

```kotlin
keys.directionState()    // SceneOffset from WASD/arrow keys
keys.zoomState()         // Float from +/- keys
keys.hasLeft/Right/Up/Down  // Boolean shortcuts
Key.displayName          // Human-readable label
```

## Gotchas

- Key repeat from the OS is swallowed; `onKeyPressed` fires exactly once per physical press
- Web: test with physical hardware — many `Key(-1)` returns for non-ASCII keys
- Android: the 70 ms debounce means very short key taps may be missed
- Do not allocate inside `handleActiveKeys` — it runs every tick