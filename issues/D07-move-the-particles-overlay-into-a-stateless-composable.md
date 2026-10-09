# Move `ParticlesDemoManager`'s overlay UI into `ui/ParticlesDemoOverlay.kt`, taking only state and callbacks.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/managers/ParticlesDemoManager.kt`, `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ui/ParticlesDemoOverlay.kt` (new)

## Problem
After D06, `ParticlesDemoManager` (a `Manager`, `ParticleEmitter` and actor) still hosts its whole overlay in `override fun Composable(windowInsets: WindowInsets) = Column(` (originally :94–135: Column(fillMaxSize, windowInsetsPadding, padding 16) > InfoPanel > Spacer(weight) > Box > `this@Column.AnimatedVisibility` > Box > `EmitterPropertiesPanel`, plus the brush `FloatingButton`). A Manager owning a UI tree is the god-object shape the code style rules out.

## Fix
1. Create `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ui/ParticlesDemoOverlay.kt` (license header from a sibling) with `@Composable internal fun ParticlesDemoOverlay(windowInsets: WindowInsets, areControlsExpanded: Boolean, emissionRate: Float, onEmissionRateChanged: (Float) -> Unit, isEmittingContinuously: Boolean, onEmittingContinuouslyChanged: () -> Unit, onBurstButtonPressed: () -> Unit, lifespan: Float, onLifespanChanged: (Float) -> Unit, onControlsToggled: () -> Unit)`.
2. Its body is the override's body verbatim (same Column modifiers, `InfoPanel(stringResource = Res.string.description, isVisible = StateHolder.isInfoPanelVisible.value)`, `Spacer`, `Box(Modifier.fillMaxWidth())`, `this@Column.AnimatedVisibility` with the same enter/exit specs and `TransformOrigin(1f, 1f)`, the inner Box, `EmitterPropertiesPanel(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp).width(240.dp), …)` and the `FloatingButton`), reading parameters instead of Manager members.
3. The override becomes a call to `ParticlesDemoOverlay` that collects `areControlsExpanded`, `emissionRate`, `isEmittingContinuously` and `lifespan` with `collectAsState()` and passes `::setEmissionRate`, `::onEmittingContinuouslyChanged`, `::burst`, `::setLifespan`, `::toggleControlsExpanded`. Trim the Manager's imports.
4. If `examples/demo-particles/CLAUDE.md` describes the Manager as rendering the UI, add that it delegates to `ui/ParticlesDemoOverlay` (currently it does not — then leave it).

Leave `StateHolder.isInfoPanelVisible` (lane G's plan). Lane D's planned `D58` may later replace this overlay with a shared one.

## Behaviour
Same tree, wrappers and modifiers. The emitter flows are now collected in the override rather than inside the AnimatedVisibility, so a slider drag recomposes the overlay root; user-driven only, no visible change.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-particles:compileKotlinDesktop`

## Manual check
Open Particles: expand/collapse the controls (scales from the bottom-right corner), drag the sliders, toggle continuous emission, Burst.
