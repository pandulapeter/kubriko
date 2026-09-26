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
manager iterates all registered `Shader` actors and chains a `Modifier.shader` for each one whose
`shader.layerIndex == layerIndex` - a `graphicsLayer { renderEffect = ... }` for a `ContentShader`, a draw
modifier filling the layer's bounds for any other `Shader` (see ContentShader vs Shader). Shaders with `layerIndex = null`
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
`uniform(name, Int)`, `uniform(name, Float)`, `uniform(name, Float, Float)`, `uniform(name, ImageBitmap)`.
Each platform's provider remembers what every uniform was last set to (`UniformCache`, by name and raw bits)
and only hands a changed value to the platform object, which keeps the ones it was given: a set is a lookup by
name on the native side and, on the web, a copy of the name into Skia's memory a byte per call, while most of a
shader's uniforms hold still from frame to frame.

## ContentShader vs Shader
`ContentShader` reads the already-rendered layer pixels as a sampler, so it has to be a render effect on the
layer: on Android `RenderEffect.createRuntimeShaderEffect(shader, "content")`, on Skia platforms
`ImageFilter.makeRuntimeShader(..., shaderName = "content", input = null)`. Plain `Shader` generates pixels from
scratch and replaces whatever the layer would have drawn, so it is **not** a render effect: `Modifier.shader`
fills the node's bounds with it from a draw modifier (`drawGenerativeShader` - a cached Skia `Paint` holding
`RuntimeShaderBuilder.makeShader()`, or on Android a cached `Paint` holding the `RuntimeShader` itself) and never
draws the content. A render effect with nothing to read would still render into an offscreen target the size of
the layer every frame and composite that - two on Android, where a node carrying any render effect becomes a
hardware layer whose own content is rendered first and then thrown away. The fill is evaluated in the node's local
space exactly as the layer-space filter was (both see `fragCoord` in layer pixels under a translate-only
transform), and nesting behaves the same: an outer generative shader replaces everything inside it, an inner one
is what an outer `ContentShader` reads. The one difference is that translucent output is blended straight into
its target instead of being stored at 8 bits first, at most one step per channel apart. The other is that on Android the fill is no longer a hardware layer HWUI can hold on to
between frames: a game that wants a static scene cached (a paused one, say) puts it on a layer of its own rather than
relying on the render effects it happens to have.

## Platform support
- **Android**: requires API 33+ (Android 13 / TIRAMISU). `areShadersSupported = false` on older
  versions; `createRenderEffect` returns `null`, `drawGenerativeShader` draws the content instead, and the
  modifier has no visual effect.
- **Desktop, iOS, Web**: always supported (`areShadersSupported = true`); uses Skia
  `RuntimeShaderBuilder`.
- Check `ShaderManager.areShadersSupported` at runtime before exposing shader-dependent features.

## BlurShader special case
`BlurShader` does not use SKSL at all — its `shaderCode` is a no-op stub. It is intercepted
before the SKSL path and delegated to native blur: `RenderEffect.createBlurEffect` on Android,
`ImageFilter.makeBlur` (Skia) on all other platforms. Its `State.applyUniforms()` is intentionally
empty. Subclassing `BlurShader` works but cannot override the SKSL code path.

## Frame invalidation
`Modifier.shader()` reads `gameTime.value` inside the `graphicsLayer` lambda (or, for a generative shader, the
draw lambda) purely to invalidate the `Canvas` every frame. Without this read, Compose would skip recomposition
when only shader uniforms change.

## RenderEffect reuse
`Modifier.shader()` reuses the cached `RenderEffect` (or, for a generative shader, the cached paint's shader)
from `Shader.Cache` without even applying the uniforms whenever the layer size is unchanged and one of two
things holds:
- The state's `dirtinessToken` matches the cached one. It defaults to always-dirty, so this is opt-in:
  a custom shader whose uniforms only change on genuine updates (not every tick, e.g. while
  idle-throttled) overrides it with a value bumped only on real change.
- The state is one of the six built-in `collection` state types and compares equal to the cached one.
  They are immutable data classes, so value equality proves the uniforms are equal — which none of
  them can express through the token. The type check is deliberately a closed list: a custom state may
  compare by identity or hold mutable fields, so it keeps the always-dirty fallback.
Otherwise the uniforms are applied, and the effect (or shader) is still reused when the `UniformCache` saw none of
them change - an always-dirty state whose values happen to hold still costs the uniform comparisons and nothing
else.
