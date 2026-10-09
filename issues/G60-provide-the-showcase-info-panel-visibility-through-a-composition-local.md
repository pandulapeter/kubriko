# Provide the Showcase info-panel visibility through a `CompositionLocal` instead of the global `StateHolder.isInfoPanelVisible`.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples, app)
**Merges:** the games' and the demos' findings about the same global.
**Files:** `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/StateHolder.kt` (:47–49), new `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ui/LocalInfoPanelVisibility.kt`, `examples/shared/CLAUDE.md` (:25), `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcase.kt` (:117–118, the `ShowcaseContent(...)` call at :102), `app/shared/CLAUDE.md` (:43), and the nine reads (`isVisible = StateHolder.isInfoPanelVisible.value`, located by `grep -rn "StateHolder.isInfoPanelVisible" examples app --include=*.kt` at 70de96c6):
  - `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/InputTest.kt` (:61)
  - `examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/CollisionTest.kt` (:58)
  - `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/managers/AudioTestManager.kt` (:91, still inside the Manager's `Composable()` override; D30, landed in 712bf4f4, moved only the music controls out)
  - `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ui/ContentShadersOverlay.kt` (:67; D02, landed in a997dc56)
  - `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ui/ParticlesDemoOverlay.kt` (:61; D07, landed in 7d5c6312)
  - `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/ui/PerformanceDemoOverlay.kt` (:69; D12, landed in 3cc3a7e7)
  - `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/ui/PhysicsDemoOverlay.kt` (:59; D17, landed in 43702c57)
  - `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsContainer.kt` (:81; D26, landed in f7fb6e7c)
  - `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/IsometricGraphicsContent.kt` (:194; D45, landed in bc28e088)

  The `-noop` test modules (`examples/test-*-noop`) do not read it. `app/shared`'s `ShowcaseContent` (`implementation/ui/ShowcaseContent.kt:87`) and `TopBar` (`implementation/ui/TopBar.kt:78, :145`) already take `isInfoPanelVisible: Boolean` as a parameter (A10, landed in 1f8496b9) and need no change. Also the touched modules' `CLAUDE.md`. Re-run the grep at execution time in case later plans move a read.
**Challenged:** amended — the recommended `remember` at the Showcase root would reset the panel on Android configuration changes and on the Windows fullscreen toggle (`key(isInFullscreenMode)`), so the state now stays process-scoped in `app/shared` (A51's `ShowcaseSession`) and only the reads move to the CompositionLocal; removed the stale "D53" merge reference; told the executor to re-locate the reads by grep, since D02/D07/D12/D17/D26/D30/D45 move them.
**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
`StateHolder`'s `companion object { val isInfoPanelVisible = mutableStateOf(true) }` (`examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/StateHolder.kt:47–49`) is process-wide mutable Compose state: the Showcase reads and writes it at the root (`KubrikoShowcase.kt:117–118`, `isInfoPanelVisible = StateHolder.isInfoPanelVisible.value`, `toggleInfoPanelVisibility = { StateHolder.isInfoPanelVisible.value = !StateHolder.isInfoPanelVisible.value }`) and nine overlays read it from deep inside Composables and one Manager `Composable()` override (see Files) (`isVisible = StateHolder.isInfoPanelVisible.value`). It is a service-locator lookup the code style rules out, it puts UI state on a contract interface, and it makes the overlays impossible to show with a different value (previews, tests, a second window). The four games do not read it.

## Fix
- `val LocalInfoPanelVisibility = compositionLocalOf { true }` (or `staticCompositionLocalOf`) in `examples/shared`.
- The Showcase holds the state in a process-scoped place in `app/shared` — A51's `ShowcaseSession` if it has landed, otherwise a `private val` next to the other file-level Showcase state in `KubrikoShowcase.kt` (`private val selectedShowcaseEntry = mutableStateOf<ShowcaseEntry?>(null)`, `:125`) — and wraps the example content in `CompositionLocalProvider(LocalInfoPanelVisibility provides isInfoPanelVisible)`. Not a `remember`/`rememberSaveable` inside `KubrikoShowcase`: see Decision.
- Each overlay reads `LocalInfoPanelVisibility.current`. Manager `Composable(windowInsets)` overrides run inside `KubrikoViewport`'s composition, which is inside the provider, so the local reaches them.
- Remove the `companion object` from `StateHolder` and its `CLAUDE.md` bullet (`examples/shared/CLAUDE.md:25`); update `app/shared/CLAUDE.md:43`.

## Decision
- **CompositionLocal (recommended)** vs passing a `Boolean` parameter through each example's entry Composable into its overlays (more explicit, but it threads through `KubrikoViewport` → Manager `Composable()` overrides, which take only `windowInsets`, so it would need per-example state holder plumbing).
- Where the state lives. Today it lives for the process. A plain `remember` at the Showcase root does **not**: the composition is rebuilt on every Android configuration change (the Activity is recreated) and on Windows whenever fullscreen is toggled (`app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt:152` wraps the window in `key(isInFullscreenMode.value)`), so the panel would reappear after a rotation or a fullscreen toggle. `rememberSaveable` survives the Android case but not the desktop `key()` change. **Recommended:** keep it process-scoped in `app/shared` (A51's `ShowcaseSession`, which exists for exactly this reason), and only move the *reading* to the CompositionLocal. Coordinate with A51, which also edits `KubrikoShowcase.kt`.

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
