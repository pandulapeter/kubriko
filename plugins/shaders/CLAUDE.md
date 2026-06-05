<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-shaders internals

## How shaders attach to layers
`Shader` extends `LayerAware`. `layerIndex` defaults to `null` (entire scene). The engine calls
`ShaderManagerImpl.processModifier(modifier, layerIndex, gameTime)` once per render layer; the
manager iterates all registered `Shader` actors and chains a `graphicsLayer { renderEffect = ... }`
modifier for each one whose `shader.layerIndex == layerIndex`. Shaders with `layerIndex = null`
match the overlay pass (`layerIndex = null` in the call). Order within a layer follows the order
actors appear in `ActorManager.allActors`.

## Deduplication by shaderState identity
`ShaderManagerImpl` builds its shader list by checking `seenStates.add(actor.shaderState)`. If two
actors share the same `shaderState` object reference, only the first is registered. This is an
intentional deduplication guard, not a bug; ensure each shader actor holds its own state instance.

## Compiled shader caching
Each `Shader` actor owns a `Shader.Cache` that holds a platform-specific compiled program object
(`runtimeShader`) and a `ShaderUniformProvider`. On Android this is `RuntimeShader`; on
Desktop/iOS/Web it is `RuntimeShaderBuilder` (Skia). These are lazily created on first render and
reused across frames — do not recreate `Shader.Cache` each frame.

## SKSL entry point and reserved uniforms
The SKSL function signature must be `half4 main(float2 fragCoord)`. Two uniforms are automatically
set by the engine before each frame:
- `uniform float2 resolution` (`Shader.RESOLUTION`) — viewport size in pixels
- `uniform shader content` (`ContentShader.CONTENT`) — only injected for `ContentShader`

Additional uniforms are declared in the SKSL and bound by overriding
`Shader.State.ShaderUniformProvider.applyUniforms()`. Available binding methods:
`uniform(name, Int)`, `uniform(name, Float)`, `uniform(name, Float, Float)`.

## ContentShader vs Shader
`ContentShader` reads the already-rendered layer pixels as a sampler. On Android this uses
`RenderEffect.createRuntimeShaderEffect(shader, "content")`; on Skia platforms it uses
`ImageFilter.makeRuntimeShader(..., shaderName = "content", input = null)`. Plain `Shader`
generates pixels from scratch — on Android via `createShaderEffect`; on Skia via
`makeRuntimeShader(..., shaderNames = emptyArray(), inputs = emptyArray())`.

## Platform support
- **Android**: requires API 33+ (Android 13 / TIRAMISU). `areShadersSupported = false` on older
  versions; `createRenderEffect` returns `null` and the modifier has no visual effect.
- **Desktop, iOS, Web**: always supported (`areShadersSupported = true`); uses Skia
  `RuntimeShaderBuilder`.
- Check `ShaderManager.areShadersSupported` at runtime before exposing shader-dependent features.

## BlurShader special case
`BlurShader` does not use SKSL at all — its `shaderCode` is a no-op stub. It is intercepted
before the SKSL path and delegated to native blur: `RenderEffect.createBlurEffect` on Android,
`ImageFilter.makeBlur` (Skia) on all other platforms. Its `State.applyUniforms()` is intentionally
empty. Subclassing `BlurShader` works but cannot override the SKSL code path.

## Frame invalidation
`Modifier.shader()` reads `gameTime.value` inside the `graphicsLayer` lambda purely to invalidate
the `Canvas` every frame. Without this read, Compose would skip recomposition when only shader
uniforms change.
