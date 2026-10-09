# Document the undocumented public API in plugin-collision

**Kind:** docs  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-collision
**Challenged:** amended — the `transposeInto` KDoc must say `dest` cannot be the receiver (it overwrites `row1` before reading `row1.y`), and the secondary `RotationMatrix(radians)` constructor is listed.
**Files:**
- `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/extensions/CollisionMaskExtensions.kt`
- `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/extensions/SceneOffsetExtensions.kt`
- `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/implementation/RotationMatrix.kt`
- `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/mask/PointCollisionMask.kt`
- `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/mask/PolygonCollisionMask.kt`

## Problem
Public/protected declarations without KDoc at 2480325f:
- `fun Collidable.isCollidingWith(other: Collidable)` (CollisionMaskExtensions.kt:30)
- the two-argument `fun CollisionMask.collisionResultWith(other, shouldSkipAxisAlignedBoundingBoxCheck)` (:34)
- `fun SceneOffset.isCollidingWith(collisionMask: CollisionMask)` (SceneOffsetExtensions.kt:17)
- `data class RotationMatrix` and all its members (`row1`, `row2`, both `set`, `transpose`, `transposeInto`,
  `times`) — public in an `implementation` package and used by plugin-physics and by consumers through
  `PolygonCollisionMask.rotationMatrix`.
- `protected var isAxisAlignedBoundingBoxDirty` (PointCollisionMask.kt:27) and
  `protected open fun updateAxisAlignedBoundingBox(target)` (:54)
- the companion `operator fun invoke(...)` factories (PointCollisionMask.kt:67, PolygonCollisionMask.kt:184)

## Fix
KDoc only (P02 has already moved the private narrow phase out of `CollisionMaskExtensions.kt`; do not move anything):
- `isCollidingWith`: whether the two Collidables' masks overlap; boolean-only and allocation-free (delegates to
  `hasCollisionWith`).
- two-argument `collisionResultWith`: the contact details of the overlap or `null`; allocates a new
  `CollisionResult` per hit — point at the `reusableResult` overload for per-frame use.
- `SceneOffset.isCollidingWith`: inside test against a complex mask (bounding box first), exact equality with a point
  mask's position.
- `RotationMatrix`: a 2×2 rotation matrix stored as two rows; document each member, including the secondary
  `constructor(radians: AngleRadians)`. State that `transpose()` allocates a new matrix and `transposeInto(dest)` does
  not, and that `dest` must be a different instance than the receiver (it writes `dest.row1` before reading
  `row1.y`, so transposing in place gives a wrong result; the only callers, in `PolygonCollisionMask`, pass a separate
  matrix); `times` returns the rotated vector (a value, the receiver is unchanged).
- the two protected members: the cache-invalidation contract for subclasses (set the flag when anything affecting the
  box changes; `updateAxisAlignedBoundingBox` writes the box into `target` in place).
- the `invoke` factories: what they build and their defaults.

If any other public/protected declaration in these five files still lacks KDoc, document it too.

## Behaviour
Unchanged — documentation only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:collision:compileKotlinDesktop`

## Manual check
none
