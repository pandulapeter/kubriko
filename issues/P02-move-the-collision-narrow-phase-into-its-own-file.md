# Move the private narrow phase out of `CollisionMaskExtensions.kt` into `collision/implementation/NarrowPhase.kt`

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-collision
**Files:**
- `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/extensions/CollisionMaskExtensions.kt`
- new `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/implementation/NarrowPhase.kt`
- `plugins/collision/CLAUDE.md`

## Problem
At 2480325f `CollisionMaskExtensions.kt` is 829 lines: the public collision queries (:30–275) followed by ~550 lines
of private SAT / clipping internals (:277–830) that start with

```kotlin
private val COLLISION_DETECTED = CollisionResult(
    contact = SceneOffset.Zero,
```

The code-style skill caps files at ~500 lines and wants a pure algorithm in a file named for it.

## Fix
Verbatim move (each KDoc/`//` comment travels with its declaration) of these declarations, in their current order,
into the new file `NarrowPhase.kt`, package `com.pandulapeter.kubriko.collision.implementation` (MPL-2.0 header
copied from `RotationMatrix.kt`):

| Declaration (current line) | Visibility after the move |
|---|---|
| `COLLISION_DETECTED` (:281) | private |
| `RESULT_NONE`, `RESULT_OBJECT`, `RESULT_SCRATCH` (:288, :291, :294) | **internal** (the public file passes them as `resultMode`) |
| `scratchContactNormal`, `scratchPenetration` (:296, :297) | **internal** (read by `findDeepestOverlapWith`) |
| `collisionResult(...)` (:301) | private |
| `CollisionMask.collisionCheck(...)` (:324) | **internal** |
| `CollisionMask.isPoint()` (:406) | private |
| `polygonPolygonAData`, `polygonPolygonBData`, the six `FloatArray` scratch faces with their `//` note (:408–418) | private |
| `checkCircleToCircleCollision`, `checkPointToCircleCollision`, `checkPointToPolygonCollision`, `checkCircleToPolygonCollision` (:422, :463, :499, :532) | private |
| `AxisData` (:623), `checkPolygonToPolygonCollision` (:628), `findAxisOfMinPenetration` (:743) | private |
| `selectionBias`, `incidentFaceVertexAt`, `clip`, `BIAS_RELATIVE`, `BIAS_ABSOLUTE` (:789–830) | private |

Stay in `CollisionMaskExtensions.kt`, unchanged: every public function (`isCollidingWith`, both
`collisionResultWith`, both `slidingMovement`, `depenetrationFrom`, `collidesWithAny`, `deepestCollisionWith`,
`firstCollisionWith`, `hasCollisionWith`), the private `findDeepestOverlapWith`, and the private
`deepestOverlapContactNormal` / `deepestOverlapPenetration` (:298–299). The public file then imports
`collisionCheck`, `RESULT_*`, `scratchContactNormal`, `scratchPenetration` from `collision.implementation`; trim
imports in both files to what each uses.

Name clash check: `collision.implementation` holds only `RotationMatrix` at 2480325f; none of the moved names clash.

The only text change allowed: the `COLLISION_DETECTED` KDoc says "Never escapes this file" — after the move it is
returned to `CollisionMaskExtensions.kt`, so change it to "Never escapes this plugin".

`plugins/collision/CLAUDE.md`: Key Files line "`src/commonMain/.../extensions/CollisionMaskExtensions.kt` — all
narrow-phase math" becomes the public queries, plus a new line for `implementation/NarrowPhase.kt` — the narrow
phase (SAT, clipping, the three result modes and their file-level scratch state). The Kinematic section's "file-level
scratch state" sentence (:72) stays true. Grep the repo (docs, skills, CLAUDE.md files) for other mentions of where the
narrow phase lives (only Tesselar's unrelated own `CollisionMaskExtensions.kt` matches the name).

Check the move mechanically: `git diff --color-moved=dimmed-zebra` shows only moved lines plus the visibility,
import and KDoc-word changes listed above.

## Behaviour
Unchanged: the same code with the same file-level scratch state (no new allocation, same thread-confinement as
before).

## Public API
None. Every public function stays in `CollisionMaskExtensionsKt`, so its JVM facade is unchanged; only a new
module-internal `NarrowPhaseKt` appears. Internal declarations are invisible to plugin-physics and consumers.

## Tests
The existing ones (`plugins/collision/src/commonTest` and `desktopTest`).

## Verify
`./gradlew :plugins:collision:compileKotlinDesktop :plugins:collision:desktopTest :plugins:physics:compileKotlinDesktop`

## Manual check
none
