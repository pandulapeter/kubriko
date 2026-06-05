<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# test-input

Test example that exercises the `keyboard-input` plugin and the `pointer-input` plugin together.
Only enabled in the Showcase app when `showcase.areTestExamplesEnabled=true` in `gradle.properties`.

## What it tests

- `KeyboardInputAware.handleActiveKeys` receiving the live `ImmutableSet<Key>` of all currently held keys
- `PointerInputAware` hover tracking (`PointerInputManager.hoveringPointerPosition`) and
  multi-pointer press tracking (`PointerInputManager.pressedPointerPositions`)
- Rendering pointer state as an `Overlay` (crosshair lines + circles drawn in `drawToViewport()`)
- `InputTestManager` implementing `Manager`, `KeyboardInputAware`, `PointerInputAware`, `Overlay`,
  and `Unique` simultaneously — a single object acting as the sole Actor and the custom Manager
- Distinct per-pointer colours derived from `PointerId.value` via HSV so multiple simultaneous
  touches/pointers are visually distinguishable

## Module structure

```
InputTest.kt                           — public Composable + createInputTestStateHolder() factory;
                                         renders a scrollable on-screen keyboard overlay above KubrikoViewport
implementation/
  InputTestStateHolder.kt              — sealed interface + Impl; creates PointerInputManager,
                                         KeyboardInputManager, and InputTestManager
  managers/
    InputTestManager.kt                — Manager + KeyboardInputAware + PointerInputAware + Overlay + Unique;
                                         tracks active keys as StateFlow; draws crosshair + circles for
                                         hover and each pressed pointer in drawToViewport()
  ui/
    Keyboard.kt                        — Composable that renders a full QWERTY keyboard layout;
                                         keys are highlighted when they appear in the activeKeys set;
                                         uses Key.displayName extension from the keyboard-input plugin
```

## Key patterns

- `InputTestManager` adds itself to `ActorManager` during `onInitialize` so it participates
  in the Overlay rendering pass without needing a separate Actor class.
- `activeKeys` is exposed as a `StateFlow<Set<Key>>` so `InputTest` can `collectAsState()` and
  pass it to the `Keyboard` Composable for real-time key-highlight updates.
- Hover pointer is drawn as a white circle with black outline (radius 20px); pressed pointers are
  drawn larger (radius 40px) in a hue derived from `PointerId.value * 47 mod 360`.
- The `Keyboard` Composable is scrollable both vertically and horizontally to accommodate narrow
  screens; it uses `Key.displayName` (from `plugin-keyboard-input`) for label text.
- No `ViewportManager` customisation is applied — the default `Dynamic` aspect ratio mode is used.
