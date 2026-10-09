# Document `Modifier.shader` and turn the `hasValueEquality` comment into KDoc

**Kind:** docs  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-shaders
**Files:**
- `plugins/shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/shaders/extensions/ModifierExtensions.kt`

## Problem
At 2480325f the public

```kotlin
fun <T : Shader.State> Modifier.shader(
    shader: Shader<T>,
    gameTime: State<Long>,
) = if (shader is ContentShader<*>) this then Modifier.graphicsLayer {
```

(ModifierExtensions.kt:30) has no KDoc, and it is used outside this repo (Tesselar `MiniMapLens.kt`,
`ScaledRenderLayer.kt`). The private `Shader.State.hasValueEquality` (:73–76) is documented with a `//` block instead of
KDoc.

## Fix
Run after P03 (which leaves only the extension and its internals in this file).
- KDoc on `Modifier.shader`: applies [shader] to the content of the modified node. A `ContentShader` becomes the
  node's render effect in a clipped `graphicsLayer`, rebuilt only when its state or the layer size changes; any other
  shader replaces the content, drawn as a fill of the node's bounds (the content itself is drawn instead where shaders
  are unsupported). `@param gameTime` — read on every frame so the node redraws as the game clock advances (pass the
  Kubriko instance's game time, as the engine does for layers). Wording to be checked against
  `ShaderManagerImpl`'s own call site.
- Turn the `//` block above `hasValueEquality` into a KDoc block with the same content.

## Behaviour
Unchanged — documentation only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:shaders:compileKotlinDesktop`

## Manual check
none
