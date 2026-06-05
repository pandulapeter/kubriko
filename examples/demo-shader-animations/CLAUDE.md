<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# demo-shader-animations CLAUDE.md

## What this demo demonstrates

Five full-screen animated SKSL shaders (`CloudShader`, `EtherShader`, `GradientShader`,
`NoodleShader`, `WarpShader`) each running in its own isolated Kubriko instance, with live
parameter controls and the ability to inspect the SKSL source code inline. Shows how to author
custom time-driven `Shader` actors and expose their uniforms as data-class `State`.

## Entry point and managers/plugins

`ShaderAnimationsDemo` (`@Composable`) is the entry point. `ShaderAnimationsDemoStateHolderImpl`
creates **one `Kubriko` per shader** via `ShaderAnimationDemoHolder`. Each holder owns:
- `ShaderManager` — required by every `Shader` actor
- `ShaderAnimationsDemoManager<SHADER, STATE>` — generic Manager that adds the shader to
  `ActorManager` and keeps its state up-to-date via a flow

`StateHolder.kubriko` is a `Flow<Kubriko?>` mapped from `selectedDemoType`, so the active viewport
swaps the underlying Kubriko instance when the user switches shaders.

The `ShaderManager.areShadersSupported` guard is respected: `ShaderAnimationsDemo.kt` shows a
fallback text on unsupported platforms.

## Key actor types

All five shaders follow the same structure: they implement `Shader<State>` and `Dynamic`.

**`Shader<State>` contract:**
- `shaderCode` — the raw SKSL string (defined as a `companion object` constant in each file)
- `shaderState` — mutable field holding current uniform values
- `shaderCache` — `Shader.Cache()` instance (reused across frames, no per-frame allocation)
- `layerIndex = null` — renders over all world layers (full-screen overlay)

**`Dynamic` contract:** `update()` advances `shaderState.time` from
`MetadataManager.activeRuntimeInMilliseconds % 100000 / 1000f` so the animation pauses when the
app loses focus without timestamp drift.

`updateState(state)` is called by `ShaderAnimationsDemoManager` when the user adjusts controls; it
copies new parameter values while preserving the current `time` field so the animation does not
reset.

**Shader catalogue:**
| Shader | Key parameters | SKSL technique |
|---|---|---|
| `GradientShader` | speed, dark, frequency | Single-pass sine-wave RGB |
| `CloudShader` | scale, speed, dark/light, cover, alpha, sky colours | Fractal Brownian Motion (credit: ShaderToy "drift") |
| `EtherShader` | speed, scale, focus | Iterative domain-warped noise |
| `NoodleShader` | speed, scale, color | Distorted stripe pattern |
| `WarpShader` | speed, scale | Turbulence-based UV distortion |

## Non-obvious implementation patterns

**One-Kubriko-per-shader.** Each shader runs in an isolated `Kubriko` instance so switching
shaders is a zero-cost viewport swap rather than an actor add/remove cycle. All five instances
stay alive for the lifetime of the state holder, keeping GPU shader caches warm.

**Generic `ShaderAnimationsDemoManager<SHADER, STATE>`.** The Manager is parameterised by both
the shader type and its state type. An `updater: (SHADER, STATE) -> Unit` lambda is passed at
construction time; this avoids any cast and keeps the manager reusable for all five shaders without
a sealed hierarchy.

**`ShaderAnimationDemoHolder.`** A plain non-Manager class that bundles a `Kubriko`, its
`ShaderManager`, and its `ShaderAnimationsDemoManager`. All five holders are created eagerly in
`ShaderAnimationsDemoStateHolderImpl.shaderAnimationDemoHolders` (persistent map keyed by
`ShaderAnimationDemoType` enum) so they are all available without lazy initialisation delays.

**Controls state machine.** `ControlsState` is a three-value enum (`COLLAPSED`, `EXPANDED`,
`CODE_VISIBLE`) rather than two booleans, preventing the illegal state of both panels showing
simultaneously.

## Platform-specific considerations

Requires GPU SKSL support; the demo guards with `ShaderManager.areShadersSupported` and shows a
localised fallback message. No platform-specific source files (`androidMain` etc.) exist in this
module — all shaders run on Android, Desktop, and Web where shaders are supported.
