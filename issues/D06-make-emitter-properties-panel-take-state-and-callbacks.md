# Make `EmitterPropertiesPanel` take state and callbacks instead of the whole `ParticlesDemoManager`.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ui/EmitterPropertiesPanel.kt`, `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/managers/ParticlesDemoManager.kt`

## Problem
`examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ui/EmitterPropertiesPanel.kt:27-42`:
```kotlin
internal fun EmitterPropertiesPanel(
    modifier: Modifier,
    particlesDemoManager: ParticlesDemoManager,
) = Panel(
    modifier = modifier,
) {
    EmitterControls(
        emissionRate = particlesDemoManager.emissionRate.collectAsState().value,
        …
```
A shared UI component receives a whole controller; it only adapts three flows and four method references for the private `EmitterControls`, which already takes plain values. The code style says components take state and callbacks, never a controller.

## Fix
1. Give `EmitterPropertiesPanel` the `EmitterControls` parameter list after `modifier`: `emissionRate: Float, onEmissionRateChanged: (Float) -> Unit, isEmittingContinuously: Boolean, onEmittingContinuouslyChanged: () -> Unit, onBurstButtonPressed: () -> Unit, lifespan: Float, onLifespanChanged: (Float) -> Unit`, keep the `Panel(modifier = modifier)` wrapper, and forward them to `EmitterControls`. Drop the `ParticlesDemoManager` and `collectAsState` imports.
2. In `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/managers/ParticlesDemoManager.kt` (`Composable` override, call at :118–124), collect the three flows **at the same place** — inside the `Box(Modifier.fillMaxSize())` content of the `AnimatedVisibility`, where the panel is called — and pass method references: `onEmissionRateChanged = ::setEmissionRate`, `onEmittingContinuouslyChanged = ::onEmittingContinuouslyChanged`, `onBurstButtonPressed = ::burst`, `onLifespanChanged = ::setLifespan`.

This must land before D07, which moves the override's body into `ui/`.

## Behaviour
Identical tree; the flows are collected in the same composition scope as before, so recomposition is unchanged.

## Public API
None (examples are unpublished).

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-particles:compileKotlinDesktop`

## Manual check
Open Particles: drag both sliders, toggle "emit continuously", press Burst — all behave as before.
