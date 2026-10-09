# Drop the never-registered `ShaderManager` from `ShaderAnimationsDemoStateHolderImpl`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationsDemoStateHolderImpl.kt` (after D23), `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationDemoHolder.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/ShaderAnimationsDemo.kt`

## Problem
`val shaderManager = ShaderManager.newInstance()` (originally `ShaderAnimationsDemoStateHolder.kt:94`) is never passed to any `Kubriko` instance — it exists only so `ShaderAnimationsDemo` can read `stateHolder.shaderManager.areShadersSupported` (`ShaderAnimationsDemo.kt:57`). Each `ShaderAnimationDemoHolder` already creates and registers its own `private val shaderManager = ShaderManager.newInstance(…)` (`ShaderAnimationDemoHolder.kt:23-26`). A Manager that is created but never initialized is misleading.

## Fix
- `ShaderAnimationDemoHolder`: `private val shaderManager` → `val shaderManager` (the class is `internal`).
- Impl: replace the standalone manager with `val areShadersSupported = shaderAnimationDemoHolders.values.first().shaderManager.areShadersSupported`, declared after `shaderAnimationDemoHolders`; drop the `ShaderManager` import.
- `ShaderAnimationsDemo`: `if (stateHolder.shaderManager.areShadersSupported)` → `if (stateHolder.areShadersSupported)`.

## Behaviour
`areShadersSupported` is a platform constant (`ShaderManagerImpl.kt:27`: `override val areShadersSupported = com.pandulapeter.kubriko.shaders.extensions.areShadersSupported`), identical on every instance.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-shader-animations:compileKotlinDesktop`

## Manual check
Open Shader Animations on desktop (supported) — tabs and shaders show; on a platform without SKSL support the fallback text still shows.
