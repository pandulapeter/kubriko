<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# app/desktop — Desktop JVM entry point

Compose Desktop (JVM) app launched via `fun main()` in `KubrikoShowcaseApp.kt`.

## Entry point and window setup

`application { ... }` block creates the Compose Desktop application. Initial window size is 860×660dp; minimum size is enforced via AWT `window.minimumSize = Dimension(400, 400)`.

`windowState` is stored in an internal engine extension property (`com.pandulapeter.kubriko.implementation.windowState`) so other desktop-only modules (like the Scene Editor) can access it.

## Fullscreen handling

Fullscreen is platform-specific on Desktop:

- **Windows**: The `Window` Composable is recreated via `key(isInFullscreenMode.value)` when toggling, because Windows requires `undecorated = true` + `resizable = false` for true fullscreen. `WindowPlacement.Fullscreen` alone is not sufficient on Windows.
- **macOS / Linux**: The window is never recreated; `windowState.placement = WindowPlacement.Fullscreen` is used directly, and `undecorated`/`resizable` remain unchanged.

When entering fullscreen, the previous `WindowPlacement`, `windowState.size`, window location, and AWT `window.bounds` are saved to restore them accurately on exit. A 100ms `delay` is needed before restoring `window.bounds` on exit (race condition with the Compose window re-render).

A `WindowStateListener` detects if the user exits fullscreen via OS gestures (e.g. pressing Escape on macOS) and syncs the `isInFullscreenMode` state.

## Scene Editors

The desktop app registers scene editors for examples that support them. These launch as separate windows (handled by the scene-editor tool) and write scenes directly into source directories:
- `AnnoyedPenguinsGameSceneEditor` → `examples/game-annoyed-penguins/.../files/scenes`
- `BlockysJourneyGameSceneEditor` → `examples/game-blockys-journey/.../files/scenes`
- `IsometricGraphicsDemoSceneEditor` → `examples/demo-isometric-graphics/.../files/scenes`
- `PerformanceDemoSceneEditor` → `examples/demo-performance/.../files/scenes`
- `PhysicsDemoSceneEditor` → `examples/demo-physics/.../files/scenes`

Scene editors are only compiled when `showcase.isSceneEditorEnabled = true` in `gradle.properties`; otherwise the `-noop` implementation is linked.

## Build configuration

- macOS: `.dmg` distribution; requires code signing identity `"PETER PANDULA"` and notarization env vars (`NOTARIZATION_APPLE_ID`, `NOTARIZATION_PASSWORD`, `NOTARIZATION_TEAM_ID`).
- Windows: `.exe` distribution; icon from `icon.ico`.
- Linux: `.deb` distribution; icon from `icon.png`.
- ProGuard is configured but currently disabled (`isEnabled.set(false)`) pending a configuration fix.
- `System.setProperty("apple.awt.application.name", "Kubriko Showcase")` sets the macOS menu bar app name.
