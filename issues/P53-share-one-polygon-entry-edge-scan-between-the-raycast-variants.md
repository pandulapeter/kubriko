# Share one polygon entry-edge scan between `raycastPolygonEntryDistance` and `raycastPolygonHit`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** plugin-collision
**Files:**
- `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/extensions/RaycastExtensions.kt`
- `plugins/collision/src/commonTest/kotlin/com/pandulapeter/kubriko/collision/RaycastTest.kt` (added by P52; not edited here)

## Problem
At 2480325f `raycastPolygonHit` (RaycastExtensions.kt:326–389) repeats the 45-line edge loop of
`raycastPolygonEntryDistance` (:226–277) statement for statement — same transform, same

```kotlin
// Solve origin + t * direction = edgeStart + u * edge using 2D cross products.
val denominator = unitDirectionX * edgeY - unitDirectionY * edgeX
```

same early-outs, same "Skip edges whose outward normal points along the ray" test — only to also keep the winning
edge's normal. Likewise `raycastDistance` (:96–128) and `nearestMaskAlongRay` (:153–187) duplicate the
normalise-direction and ray-bounds prologue. A future fix to one copy will miss the other (the second sweep already had
to fix a ray/segment test in physics).

## Fix
Constraints: results must come from the same float expressions in the same order (bit-identical); no file-level
scratch state (these functions are reentrant today and must stay safe to call from several threads); zero allocation
in the per-mask scan.

Options for the polygon loop:
- **A (recommended):** `private fun raycastPolygonEntryEdge(polygon, originX, originY, unitDirectionX, unitDirectionY,
  maximumDistance): Int` holds the loop verbatim and returns the winning edge index (or `-1`), tracking
  `bestDistance` locally exactly as today (so tie-breaking — first edge wins on `distance >= bestDistance` — is
  unchanged). A second private function evaluates one edge's entry distance and world-space normal from the same
  expressions. `raycastPolygonEntryDistance` = edge scan + one edge evaluation for the winner; `raycastPolygonHit` =
  edge scan + the winner's distance and normal. Cost: one extra edge evaluation (a handful of multiplies) per mask
  that is actually hit — in `raycastDistance`/`nearestMaskAlongRay` that is once per candidate mask whose polygon the
  ray enters, not per edge. Acceptable; state it in the commit.
- B: return distance and index packed into one `Long` (`floatToRawIntBits` + index) — no recomputation, but obscure.
- C: leave as is.

For the prologue: a private inline helper is not possible without returning several floats; leave the two prologues
duplicated unless option A's shape suggests a natural split (do not introduce scratch state for it).

Convert the `//` comments on the private functions (`nearestMaskAlongRay` :150–152, `raycastEntryDistance` :189–190,
`raycastHit` :279–280) to KDoc in the same commit only if the functions they document change; otherwise leave them for
a docs plan.

## Behaviour
Unchanged: the winning edge is chosen by the same comparisons in the same order, and its distance and normal are
recomputed with the identical expressions, so every result — including Tesselar's `raycastDistance` calls — is
bit-identical.

## Public API
None — only private functions change.

## Tests
Run after P52, whose `RaycastTest` pins today's results (plugin-collision had no raycast tests at 2480325f, and Tesselar
calls `raycastDistance` from `Dog.kt` and `NonPlayerCharacter.kt`). Those tests must pass unchanged after the refactor;
add none here. Drop this plan if P52 has not landed.

## Verify
`./gradlew :plugins:collision:compileKotlinDesktop :plugins:collision:desktopTest`

## Manual check
none
