# Share the "hide the loading overlay 300 ms after the actors arrive" logic between the demos that repeat it.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/managers/PerformanceDemoManager.kt`, `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/managers/PhysicsDemoManager.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/logic/manager/LogicManager.kt`, `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/…` (new helper)

## Problem
Three Managers hand-roll the same pattern with slightly different filters:
- `PerformanceDemoManager.onInitialize` (:71–77): `allActors.filter { it.isNotEmpty() }.onEach { delay(300); _shouldShowLoadingIndicator.update { false } }.launchIn(scope)`
- `PhysicsDemoManager.onInitialize` (:96–103): `.filter { it.size > 1 }.distinctUntilChanged().onEach { … }` (the Manager itself is always one actor)
- `LogicManager.onInitialize` (:52–58): `.filter { it.isNotEmpty() }.take(1).onEach { … }` — and `LogicManager` is synced Tesselar gameplay code.
Performance and physics also re-show the indicator in `processJson` (`_shouldShowLoadingIndicator.update { true }`), so their variants are not equivalent to `take(1)`.

## Fix
A small helper in `examples/shared` (lane G's module), e.g. `fun Flow<List<Actor>>.dismissLoadingWhen(scope, isLoaded: (List<Actor>) -> Boolean, onLoaded: () -> Unit)`, used by performance and physics; it could also fold into lane G's scene-editor connection plan, which owns the same two Managers' scene loading.

## Decision
(a) helper in examples/shared for performance + physics only, leaving the Tesselar-synced `LogicManager` alone — recommended; (b) also LogicManager (sync cost); (c) fold into lane G's scene-editor connection plan.

## Behaviour
Same timing and filters per demo.

## Public API
None.

## Tests
A `runTest` test of the helper with virtual time (300 ms).

## Verify
`./gradlew :examples:shared:desktopTest :examples:demo-performance:compileKotlinDesktop :examples:demo-physics:compileKotlinDesktop`

## Manual check
Performance and Physics: the loading overlay fades shortly after the scene appears, and again after a scene-editor reload.
