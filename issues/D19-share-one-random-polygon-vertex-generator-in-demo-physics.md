# Share one random-polygon vertex generator between the physics state holder and `PhysicsDemoManager`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/PolygonShapes.kt` (new), `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/PhysicsDemoStateHolderImpl.kt` (after D14), `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/managers/PhysicsDemoManager.kt`

## Problem
The same block is copied in the `StaticPolygon` editor factory (originally `PhysicsDemoStateHolder.kt:83-91`) and in `PhysicsDemoManager.createDynamicPolygon` (`:233-241`):
```kotlin
(3..10).random().let { sideCount ->
    (0..sideCount).map { sideIndex ->
        val angle = AngleRadians.TwoPi / sideCount * (sideIndex + 0.75f)
        SceneOffset(
            x = (30..120).random().sceneUnit * angle.cos,
            y = (30..120).random().sceneUnit * angle.sin,
        )
    }
}
```

## Fix
Create `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/PolygonShapes.kt` (license header) with `internal fun randomPolygonVertices(): List<SceneOffset> =` that expression **verbatim** (including `0..sideCount`, which yields `sideCount + 1` vertices — not a fix target here), and call it from both sites (`vertices = randomPolygonVertices(),`). Trim the now-unused imports (`AngleRadians`, `cos`, `sin`, maybe `sceneUnit`) in both files. (test-collision has a similar generator with `10..40` radii — a different module, leave it.)

## Behaviour
Same expression, same random calls in the same order.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-physics:compileKotlinDesktop`

## Manual check
none
