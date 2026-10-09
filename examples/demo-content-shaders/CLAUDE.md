<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# demo-content-shaders CLAUDE.md

## What this demo demonstrates

Shows the built-in `ContentShader` post-processing effects from the `plugin-shaders` module applied
on top of a live animated scene. Users can toggle six independent shader effects at runtime via an
expandable controls panel.

## Entry point and managers/plugins

`ContentShadersDemo` (top-level `@Composable`) is the entry point. It gates rendering on
`ShaderManager.areShadersSupported`; unsupported platforms display a localized message instead.

`ContentShadersDemoStateHolderImpl` creates:
- `ViewportManager` — `AspectRatioMode.Stretched(2000×2000 su)` so the canvas always fills the screen
- `ShaderManager` — required by every shader actor
- `ContentShadersDemoManager` — custom Manager that owns the actor and shader lifecycle; its `Composable` override delegates to `ui/ContentShadersOverlay`

## Key actor types

**`ColorfulBox`** (`Visible`, `Dynamic`) — 21×21 grid of 100×100 su boxes arranged around the
origin. Each box cycles its HSV hue forward at `deltaTime / 10` degrees per millisecond,
creating a constantly shifting colour field behind the shaders. The border is hidden while the
`ComicShader` is active (it overrides outlines visually).

**Shader actors** (from `plugin-shaders` built-in collection, added/removed as `Actor` instances):
`BlurShader`, `ChromaticAberrationShader`, `ComicShader`, `RippleShader`, `SmoothPixelationShader`,
`VignetteShader`. Default-enabled: `VignetteShader`, `RippleShader`, `ChromaticAberrationShader`.

## Non-obvious implementation patterns

**Shaders are Actors.** Each shader is added to / removed from `ActorManager` via a `state.onEach`
flow inside `onInitialize`. When the user toggles a switch the whole active set is rebuilt: all
existing `Shader` instances are first `remove()`d, then the newly enabled ones are `add()`d. The
shader objects themselves are held as `lazy` properties so they are reused across toggles (no
re-allocation).

**`shouldDrawBorder` lambda on `ColorfulBox`.** Each `ColorfulBox` receives one capturing
`shouldDrawBorder` lambda (`{ !state.value.isComicShaderEnabled }`), created once at setup rather
than per frame and evaluated inside `draw()`. This keeps `ColorfulBox` free of direct Manager
dependencies.

**Platform guard.** The demo checks `ShaderManager.areShadersSupported` at the `@Composable` level
and shows a fallback text on platforms where SKSL shaders are unavailable.

## Platform-specific considerations

Shaders require GPU SKSL support. The `areShadersSupported` guard handles graceful fallback.
No platform-specific source files (`androidMain`, `desktopMain`, etc.) exist in this module.
