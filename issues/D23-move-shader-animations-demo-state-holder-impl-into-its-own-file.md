# Move `ShaderAnimationsDemoStateHolderImpl` out of `ShaderAnimationsDemoStateHolder.kt` into `ShaderAnimationsDemoStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationsDemoStateHolder.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationsDemoStateHolderImpl.kt` (new)

## Problem
`examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationsDemoStateHolder.kt` holds `sealed interface ShaderAnimationsDemoStateHolder : StateHolder` (:54, with its 22-string resource-gate companion) and `internal class ShaderAnimationsDemoStateHolderImpl(` (:90–153). One top-level type per file.

## Fix
- Create `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationsDemoStateHolderImpl.kt` (license header, same package) and move the whole Impl verbatim (there is no `LOG_TAG` in this file).
- The old file keeps the interface and companion; split the imports (the Impl takes the five shader imports, `ControlsState`, `ShaderManager`, `toPersistentMap`, `MutableStateFlow`, `asStateFlow`, `map`, `update`; the interface keeps `Composable`, `StateHolder`, `preloadedImageVector`, `preloadedString`, `Res` and the resource accessors).
- Grep the repo for `ShaderAnimationsDemoStateHolder.kt`; fix any path reference in the same commit.

## Behaviour
Verbatim move within one package.

## Public API
None (examples are unpublished).

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-shader-animations:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
