# Delete `DraggableCollidableActor`'s duplicate `draw()` override and make `collisionTestManager` private.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/actors/DraggableCollidableActor.kt`, `examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/CollisionTestStateHolderImpl.kt` (after D31)

## Problem
- `DraggableCollidableActor.draw()` (`examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/actors/DraggableCollidableActor.kt:34-37`) is character-for-character the inherited `DraggableActor.draw()` (`DraggableActor.kt:96-99`):
  ```kotlin
  override fun DrawScope.draw() = with(collisionMask) {
      drawDebugBounds(if (collisions.isNotEmpty()) Color.DarkGray else Color.Gray, Fill)
      drawDebugBounds(Color.Black, Stroke())
  }
  ```
- `val collisionTestManager = CollisionTestManager()` in `CollisionTestStateHolderImpl` is public but read only inside the Impl (grep `collisionTestManager` repo-wide).

## Fix
Delete the override from `DraggableCollidableActor` and its now-unused imports (`Color`, `DrawScope`, `Fill`, `Stroke` — check `newRandomShape` doesn't use them); change the val to `private val`. Make sure `DraggableActor.draw` is `open`/non-final as today (it is overridden now, so it is).

## Behaviour
The inherited implementation is identical; `RayEmitter` keeps its own override.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:test-collision:compileKotlinDesktop`

## Manual check
Collision test: shapes are grey, turn dark grey while overlapping, black outline.
