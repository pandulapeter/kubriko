# Delete demo-performance's dead code and narrow the visibility of its actors and Manager property.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/ui/MiniMap.kt`, `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/actors/Camera.kt`, `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/actors/BoxWithCircle.kt`, `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/PerformanceDemoStateHolderImpl.kt` (after D09)

## Problem
- `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/ui/MiniMap.kt:57`: a leftover `//.map {  to  }` between `allVisibleActors` and `.forEach { actor ->`.
- `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/actors/Camera.kt:35-36`: `private lateinit var actorManager: ActorManager` and `private lateinit var stateManager: StateManager` are assigned in `onAdded` (`actorManager = kubriko.get()`, `stateManager = kubriko.get()`) and never read (grep the file).
- `class Camera private constructor(state: State)` (:32) and `class BoxWithCircle private constructor(state: State)` (`BoxWithCircle.kt:29`) are public, while their sibling `internal class MovingBox private constructor(state: State)` already proves the scene editor and serialization work with internal actors. Nothing outside the module references them (grep `demoPerformance.implementation.actors` repo-wide; app/ only imports `PerformanceDemoStateHolder`).
- `val performanceDemoManager by lazy {` in `PerformanceDemoStateHolderImpl` is public but only read inside the Impl (grep `performanceDemoManager` repo-wide).

## Fix
- Delete the `//.map {  to  }` line (keep the `allVisibleActors` / `.forEach` chain as is).
- In `Camera`: delete the two lateinits, their two assignments in `onAdded`, and the now-unused `ActorManager` / `StateManager` imports. Keep `viewportManager` and `update(0)`.
- Make `Camera` and `BoxWithCircle` `internal class` (their nested `State` classes follow).
- Make `performanceDemoManager` `private val`.

## Behaviour
Nothing reads the removed members; `kubriko.get()` of a registered Manager has no side effect. Visibility changes compile-time only.

## Public API
None (examples are unpublished).

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-performance:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
Desktop: open the Performance demo's Scene Editor, check `Camera` and `BoxWithCircle` still appear in the type list and a save/load round-trips.
