# Move `ContentShadersDemoStateHolderImpl` out of `ContentShadersDemoStateHolder.kt` into `ContentShadersDemoStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ContentShadersDemoStateHolder.kt`, `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ContentShadersDemoStateHolderImpl.kt` (new)

## Problem
`examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ContentShadersDemoStateHolder.kt` holds two top-level types: `sealed interface ContentShadersDemoStateHolder : StateHolder` (:37, with its `companion object` resource gate) and `internal class ContentShadersDemoStateHolderImpl(` (:60–95), plus `private const val LOG_TAG = "ContentShaders"` (:97). The code style asks for one top-level type per file, named after it. (Lane G does the same split for the four `game-*` modules.)

## Fix
- Create `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ContentShadersDemoStateHolderImpl.kt` (MPL-2.0 header copied from the old file, package `com.pandulapeter.kubriko.demoContentShaders.implementation`).
- Move verbatim into it: the whole `internal class ContentShadersDemoStateHolderImpl` and `private const val LOG_TAG` (stays `private`; only the Impl uses it — grep the module for `LOG_TAG`).
- The old file keeps the interface with its companion (`areResourcesLoaded`, `areIconResourcesLoaded`, `areStringResourcesLoaded`).
- Split the imports so each file keeps exactly what it uses (the interface file keeps `Composable`, `StateHolder`, `preloadedImageVector`, `preloadedString`, `Res` and the string/drawable accessors; the Impl file gets `Kubriko`, `ContentShadersDemoManager`, `sceneUnit`, `ViewportManager`, `ShaderManager`, `SceneSize`, `MutableStateFlow`, `asStateFlow`).
- A sealed interface may be implemented in another file of the same package and module. Callers import the Impl by package, which does not change.
- Grep the repo (CLAUDE.md files, skills, docs) for `ContentShadersDemoStateHolder.kt` and fix any path reference in the same commit.

## Behaviour
Verbatim move within one package; nothing else changes.

## Public API
None (examples are unpublished).

## Tests
The existing ones (the module has none).

## Verify
`./gradlew :examples:demo-content-shaders:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
