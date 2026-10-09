# Decide whether test-collision's unused `RayEmitter` is re-enabled (with real ray casts) or deleted, and remove the commented-out code either way.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:** `examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/actors/RayEmitter.kt`, `examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/managers/CollisionTestManager.kt`, `examples/test-collision/src/commonMain/composeResources/values/strings.xml`, `examples/test-collision/CLAUDE.md`

## Problem
- `CollisionTestManager.onInitialize` ends with `} // + RayEmitter()` (`CollisionTestManager.kt:32`).
- `strings.xml` (:14) keeps a commented-out alternative: `<!--    <string name="description">Drag the different shapes around to test collision detection and ray tracing.</string>-->`.
- `RayEmitter` (`RayEmitter.kt:24`, `internal class RayEmitter : DraggableActor(`) draws 32 outward rays but never casts one; `examples/test-collision/CLAUDE.md` describes it at :22 ("currently commented out in `CollisionTestManager`"), :39 ("currently unused") and :52.
- `RADIUS_AROUND_RAY_EMITTER = 24` (`CollisionTestManager.kt:38`, used at :28–29) only makes sense with it (it keeps spawned shapes off the origin).
- The second review sweep still owes a manual check of physics ray casting (memory), which bears on whether a ray demo is wanted here.

## Fix
Option A: re-enable — add `RayEmitter()` to the initial actors, make it cast against the `CollisionManager`/physics ray API and draw hits, and restore the "ray tracing" description. Option B (**recommended** unless the user wants a ray test page): delete `RayEmitter.kt`, the `// + RayEmitter()` comment, the commented-out string and the CLAUDE.md lines; rename `RADIUS_AROUND_RAY_EMITTER` to what it now does (e.g. `MINIMUM_DISTANCE_FROM_ORIGIN`) or keep the value inline.

## Decision
A or B — recommended B.

## Behaviour
B: none (the class is never instantiated). A: a new actor appears at the origin.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:test-collision:compileKotlinDesktop`

## Manual check
A only: drag the emitter, rays stop at shapes.
