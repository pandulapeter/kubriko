# Extract the sweep-and-prune broad phase from `PhysicsManagerImpl` into an internal class with a direct pair-order test

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** plugin-physics
**Files:**
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/PhysicsManagerImpl.kt`
- new `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/implementation/SweepAndPrune.kt`
- new `plugins/physics/src/commonTest/kotlin/com/pandulapeter/kubriko/physics/SweepAndPruneTest.kt`
- `plugins/physics/CLAUDE.md`

## Problem
`PhysicsManagerImpl` (331 lines at 2480325f) holds two responsibilities: the fixed-timestep integrator and a
self-contained broad-phase algorithm with its own state

```kotlin
private var sweepBodiesSnapshot: List<PhysicsBody>? = null
private var sweepSortedIndices = IntArray(0)
private var sweepMinX = FloatArray(0)
private var sweepMaxX = FloatArray(0)
private var sweepPairs = LongArray(0)
```

(:58–62) and logic (`broadPhaseCheck`, :228–303). `plugins/physics/CLAUDE.md` calls its pair order load-bearing
("restoring the exact `(i, j)` order of a naive nested loop … Do not optimize away the pair sort"), yet that order is
only exercised end-to-end (`SweepAndPruneNanTest` runs a whole Kubriko and checks that pairs resolve, not their order).

## Fix
Run after P07 (which rewrites the comments being moved). Re-locate code by the quotes.

1. New `internal class SweepAndPrune` in `physics.implementation` (MPL header from a sibling) holding the five fields
   above, moved verbatim, and:
   - `fun findPairs(bodies: List<PhysicsBody>): Int` — the body of `broadPhaseCheck` from `val bodyCount = bodies.size`
     through `sweepPairs.sort(fromIndex = 0, toIndex = pairCount)`, verbatim (returns 0 for fewer than two bodies,
     returns `pairCount` at the end).
   - `fun firstBodyIndexAt(pairIndex: Int): Int = (sweepPairs[pairIndex] ushr 32).toInt()` and
     `fun secondBodyIndexAt(pairIndex: Int): Int = (sweepPairs[pairIndex] and 0xFFFFFFFFL).toInt()` — two primitive
     accessors, so the caller boxes nothing (recommended over returning the packed `Long`, which would leak the
     packing to the manager).
   - The KDoc of `broadPhaseCheck` moves to the class; the in-body comments move with their statements.
2. `PhysicsManagerImpl`: `private val sweepAndPrune = SweepAndPrune()`; `broadPhaseCheck()` becomes

   ```kotlin
   private fun broadPhaseCheck() {
       val bodies = rigidBodies.value
       val pairCount = sweepAndPrune.findPairs(bodies)
       for (k in 0 until pairCount) {
           narrowPhaseCheck(
               bodyA = bodies[sweepAndPrune.firstBodyIndexAt(k)],
               bodyB = bodies[sweepAndPrune.secondBodyIndexAt(k)],
           )
       }
   }
   ```

   Remove the `isOverlapping` import from the manager if unused.
3. `plugins/physics/CLAUDE.md` → "Broad Phase: Sweep-and-Prune": name `implementation/SweepAndPrune.kt` and
   `findPairs` where it says `broadPhaseCheck()`; add the file to Key Files. Grep the repo for `broadPhaseCheck`
   (CLAUDE.md files, docs, skills) and update the references.

## Behaviour
Unchanged: the same statements over the same primitive arrays in the same order; `bodies` is read once per step, as
before (the manager reads `rigidBodies.value` once and hands the same list in). The snapshot identity check
(`bodies !== sweepBodiesSnapshot`) still sees the same list reference. No per-frame allocation is added (one object
created with the manager; accessor calls on primitives).

## Public API
None.

## Tests
`SweepAndPruneTest` (commonTest; bodies built as in `RayIntersectionTest`, no drawing):
- for several seeded random layouts (e.g. 50 circles and boxes, positions in a 500×500 area, some overlapping, a few
  static via `density = 0f` and a few `isParticle = true`), `findPairs` returns exactly the pairs of the naive loop
  `for (i in bodies.indices) for (j in i + 1 until bodies.size)` filtered by the same skip rules
  (`invMass == 0f` on both, `isParticle` on both) and `axisAlignedBoundingBox.isOverlapping`, **in the same order**;
- calling it a second time after moving a few bodies (temporal-coherence path) still matches;
- a body whose position is `NaN` produces no pair and does not change the other pairs.
`invMass`/`isParticle` are internal and visible to the module's tests.

## Verify
`./gradlew :plugins:physics:compileKotlinDesktop :plugins:physics:desktopTest`

## Manual check
`demo-physics` and Annoyed Penguins on desktop: stacks settle and launches behave as before.
