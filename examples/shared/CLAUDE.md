<!--
  This file is part of Kubriko.
  Copyright (c) Pandula Péter 2025-2026.
  https://github.com/pandulapeter/kubriko

  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
  If a copy of the MPL was not distributed with this file, You can obtain one at
  https://mozilla.org/MPL/2.0/.
-->
# examples/shared

This module provides the minimal cross-cutting contract and utilities that every example and game module in the Showcase app must satisfy so the Showcase shell can manage them uniformly.

## What lives here

### `StateHolder` (`commonMain`)

The single shared interface every example's state holder implements. It defines:

- `val kubriko: Flow<Kubriko?>` — the active `Kubriko` instance, exposed as a `Flow` so the debug menu in the Showcase shell can observe and attach to it. Nullable to signal "not yet initialized".
- `val backNavigationIntent: Flow<Unit>` — emits whenever the example wants to trigger back navigation (e.g. after the user confirms exit). Default returns `emptyFlow()`.
- `fun stopMusic()` — called by the Showcase shell just before a crossfade transition begins, so music stops slightly earlier than `dispose()` to avoid the audio cutting out mid-fade. Default is no-op.
- `fun navigateBack(isInFullscreenMode, onFullscreenModeToggled): Boolean` — hook for the system back gesture/button; returns `true` if the event was consumed (e.g. to pause instead of exit). Default returns `false`.
- `fun dispose()` — releases all Kubriko instances and associated resources.
- `companion object { val isInfoPanelVisible = mutableStateOf(true) }` — shared Compose state that the Showcase app's info panel observes to show/hide contextual help text. Stored here so all examples can write to it without depending on the app module.

### `ResourceLoader.web.kt` (`webMain`)

A single `getFixedUri(path, rootPathName)` utility function for constructing absolute audio/asset URIs on Wasm/JS targets. The function reads `window.location.pathname` and resolves the deploy root path so that audio preloading works correctly whether the Showcase app is served at the root or a sub-path. All example modules that load audio on Web delegate URI construction to this function.

## Who uses this module

All game and demo example modules depend on `examples/shared`:
`game-annoyed-penguins`, `game-blockys-journey`, `game-space-squadron`, `game-wallbreaker`, and all `demo-*`, `test-*`, and `test-*-noop` modules.

## Why it exists as a separate module

The Showcase shell (`app/shared`) depends on every example module, but example modules must not depend on the app. `examples/shared` sits below both, providing the shared contract without creating a circular dependency. Platform-specific URI handling (`ResourceLoader.web.kt`) is also shared here to avoid duplicating the same workaround in every example that loads audio on Web.
