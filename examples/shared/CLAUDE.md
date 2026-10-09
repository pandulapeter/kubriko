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

### `ui/GameRipple.kt` and `ui/GameButton.kt` (`commonMain`)

The games style their ripples more strongly than Material 3 allows: Material 3 removed the ripple alpha from `RippleConfiguration`, and its own components build their ripple inside `Surface`, where a theme can no longer reach it.

- `gameRipple(color, rippleAlpha)` — an `IndicationNodeFactory` built on the public `createRippleModifierNode`, carrying the exact alphas each game's theme is designed around. Themes provide it via `LocalIndication`.
- `GameButton(...)` — a `FloatingActionButton` replacement: a non-clickable `Surface` (same shape, colors and 6dp/8dp hover elevation) wrapping a `Box` whose `clickable` takes its indication from `LocalIndication`. This puts the state layer above the container but below the content, exactly where Material draws it, while keeping the custom alphas.

Themes still provide `LocalRippleConfiguration` with the non-deprecated single-argument `RippleConfiguration(color)` so any Material component that builds its own ripple (currently only Annoyed Penguins' `Slider`) keeps the right ripple color.

### `ui/GameHover.kt` and `ui/GameTypography.kt` (`commonMain`)

Styling helpers every game's buttons and theme would otherwise repeat:

- `Modifier.gameHover(onEnter, onExit)` — calls the latest callbacks when a hovering pointer enters or leaves the element. Each game button keeps its own highlight state and plays its hover sound from `onEnter`.
- `Typography.withFontFamily(fontFamily)` — a copy of the typography with all fifteen text styles in the given font. Each theme loads its font in its own `@Composable` and calls `Typography().withFontFamily(...)`.

### `ui/ExpandableControlsOverlay.kt` and `ui/ExpandControlsButton.kt` (`commonMain`)

- `ExpandableControlsOverlay(windowInsets, description, isExpanded, onToggle, panel)` — the overlay of the demos with a controls panel (Content Shaders, Particles): the info panel on top, and a panel that scales in and out of the bottom-end corner above the brush button. `panel` receives the modifier that places it.
- `ExpandControlsButton(modifier, isExpanded, onToggle)` — that brush button on its own, also used by Shader Animations' control buttons.
- `areExpandControlsButtonResourcesLoaded()` (`ui/ExpandControlsButtonResources.kt`) — preloads the button's icon and strings; the resource gate of every example using it calls it.

### `ui/ShadersNotSupportedMessage.kt` (`commonMain`)

`ShadersNotSupportedMessage(modifier, windowInsets)` — the centred fallback text the two shader demos (Content Shaders, Shader Animations) show when `ShaderManager.areShadersSupported` is false; `areShadersNotSupportedMessageResourcesLoaded()` (`ui/ShadersNotSupportedMessageResources.kt`) preloads its string.

This module has its own compose resources (`ic_brush`, `expand_controls`, `collapse_controls`, `shaders_not_supported`) for these components. Its `Res` class stays internal, so examples reach the resources only through the components and their preload functions.

### `ResourceLoader.web.kt` (`webMain`)

A single `getFixedUri(path, rootPathName)` utility function for constructing absolute audio/asset URIs on Wasm/JS targets. The function reads `window.location.pathname` and resolves the deploy root path so that audio preloading works correctly whether the Showcase app is served at the root or a sub-path. All example modules that load audio on Web delegate URI construction to this function.

## Who uses this module

All game and demo example modules depend on `examples/shared`:
`game-annoyed-penguins`, `game-blockys-journey`, `game-space-squadron`, `game-wallbreaker`, and all `demo-*`, `test-*`, and `test-*-noop` modules.

## Why it exists as a separate module

The Showcase shell (`app/shared`) depends on every example module, but example modules must not depend on the app. `examples/shared` sits below both, providing the shared contract without creating a circular dependency. Platform-specific URI handling (`ResourceLoader.web.kt`) is also shared here to avoid duplicating the same workaround in every example that loads audio on Web.
