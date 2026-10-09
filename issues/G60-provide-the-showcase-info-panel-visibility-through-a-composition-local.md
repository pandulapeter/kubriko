# Provide the Showcase info-panel visibility through a `CompositionLocal` instead of the global `StateHolder.isInfoPanelVisible`.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples, app)
**Merges:** the games' and the demos' findings about the same global.
**Files:** `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/StateHolder.kt`, new `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ui/LocalInfoPanelVisibility.kt`, `examples/shared/CLAUDE.md`, `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcase.kt` (:116–117), `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/InputTest.kt` (:61), `examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/CollisionTest.kt` (:58), `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/managers/AudioTestManager.kt` (:112), `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/managers/ContentShadersDemoManager.kt` (:135), `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/managers/ParticlesDemoManager.kt` (:103), `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/managers/PerformanceDemoManager.kt` (:96), `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/managers/PhysicsDemoManager.kt` (:124), `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsContainer.kt` (:82), `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/IsometricGraphicsContent.kt` (:242); the `-noop` test modules if they read it; the touched modules' `CLAUDE.md`. Lane D's Now plans move most of these reads before this plan runs — D02 (`ui/ContentShadersOverlay.kt`), D07 (`ui/ParticlesDemoOverlay.kt`), D12 (`ui/PerformanceDemoOverlay.kt`), D17 (`ui/PhysicsDemoOverlay.kt`), D26/D45 (`ControlsContainer.kt`, `IsometricGraphicsContent.kt` restructured), D30 (`AudioTestManager.kt`) — so locate every read at execution time with `grep -rn "StateHolder.isInfoPanelVisible" examples app` instead of using the line numbers above; the same goes for A10/A51's changes to `KubrikoShowcase.kt`
**Challenged:** amended — the recommended `remember` at the Showcase root would reset the panel on Android configuration changes and on the Windows fullscreen toggle (`key(isInFullscreenMode)`), so the state now stays process-scoped in `app/shared` (A51's `ShowcaseSession`) and only the reads move to the CompositionLocal; removed the stale "D53" merge reference; told the executor to re-locate the reads by grep, since D02/D07/D12/D17/D26/D30/D45 move them.

## Problem
`StateHolder`'s `companion object { val isInfoPanelVisible = mutableStateOf(true) }` (`examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/StateHolder.kt:47–48`) is process-wide mutable Compose state: the Showcase writes it (`KubrikoShowcase.kt:117`, `StateHolder.isInfoPanelVisible.value = !StateHolder.isInfoPanelVisible.value`) and nine overlays read it from deep inside Composables and Manager `Composable()` overrides (`isVisible = StateHolder.isInfoPanelVisible.value`). It is a service-locator lookup the code style rules out, it puts UI state on a contract interface, and it makes the overlays impossible to show with a different value (previews, tests, a second window). The four games do not read it.

## Fix
- `val LocalInfoPanelVisibility = compositionLocalOf { true }` (or `staticCompositionLocalOf`) in `examples/shared`.
- The Showcase holds the state in a process-scoped place in `app/shared` — A51's `ShowcaseSession` if it has landed, otherwise a `private val` next to the other file-level Showcase state in `KubrikoShowcase.kt` — and wraps the example content in `CompositionLocalProvider(LocalInfoPanelVisibility provides isInfoPanelVisible)`. Not a `remember`/`rememberSaveable` inside `KubrikoShowcase`: see Decision.
- Each overlay reads `LocalInfoPanelVisibility.current`. Manager `Composable(windowInsets)` overrides run inside `KubrikoViewport`'s composition, which is inside the provider, so the local reaches them.
- Remove the `companion object` from `StateHolder` and its `CLAUDE.md` bullet.

## Decision
- **CompositionLocal (recommended)** vs passing a `Boolean` parameter through each example's entry Composable into its overlays (more explicit, but it threads through `KubrikoViewport` → Manager `Composable()` overrides, which take only `windowInsets`, so it would need per-example state holder plumbing).
- Where the state lives. Today it lives for the process. A plain `remember` at the Showcase root does **not**: the composition is rebuilt on every Android configuration change (the Activity is recreated) and on Windows whenever fullscreen is toggled (`KubrikoShowcaseApp.kt` wraps the window in `key(isInFullscreenMode.value)`), so the panel would reappear after a rotation or a fullscreen toggle. `rememberSaveable` survives the Android case but not the desktop `key()` change. **Recommended:** keep it process-scoped in `app/shared` (A51's `ShowcaseSession`, which exists for exactly this reason), and only move the *reading* to the CompositionLocal. Coordinate with A51, which also edits `KubrikoShowcase.kt`.

## Behaviour
Same toggle, same default (`true`), shared by every example as today. Check that `KubrikoViewport` does not break the composition-local chain on any platform (it composes Manager content in the same composition).

## Public API
None published (`examples/shared` and `app` are unpublished).

## Tests
None (UI wiring).

## Verify
`./gradlew :examples:shared:compileKotlinDesktop`, each touched example module's `compileKotlinDesktop`, and `./gradlew :app:desktop:compileKotlin`; run with `showcase.areTestExamplesEnabled=true` so the `test-*` modules (not their `-noop` twins) compile.

## Manual check
In the Showcase, toggle the info button in the top bar while on each demo and test example: every info panel shows/hides as before, and the state carries over when switching examples.
