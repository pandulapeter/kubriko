# Keep the physics sweep-and-prune sorted when a body's bounds are NaN

**Challenged:** amended — the test as written passed at HEAD (pairs added in ascending x order are already sorted, so the NaN barrier never splits a pair); it now adds every pair's first body, then the NaN body, then every second body, which loses all but one pair at HEAD, and waits for *any* body to move rather than the first pair.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `plugin-physics`
**Files:** `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/PhysicsManagerImpl.kt`, `plugins/physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/physics/SweepAndPruneNanTest.kt` (new), `plugins/physics/CLAUDE.md`

## Problem

`PhysicsManagerImpl.broadPhaseCheck` (at 0008d027) insertion-sorts bodies by the left edge of their bounds and prunes with an early `break`:

```kotlin
for (i in 0 until bodyCount) {
    val aabb = bodies[i].collisionMask.axisAlignedBoundingBox
    sweepMinX[i] = aabb.left.raw
    sweepMaxX[i] = aabb.right.raw
}
...
while (m >= 0 && sweepMinX[sorted[m]] > key) {
...
if (sweepMinX[j] >= maxXa) {
    break
}
```

Every comparison with `NaN` is false, so a single body whose bounds are `NaN` (a `NaN` position from a division by zero in game code, a `0/0` impulse, a `NaN` size) is never moved by the sort and no finite key can move past it. The array is then no longer sorted, and the early `break` prunes real pairs that sort "after" a smaller key. Measured with a probe on 2000 random balls: seed 10 → 1528 contacts without the NaN body, 764 with it; seed 11 → 1627 vs 923; seed 12 → 1505 vs 861. **One bad body makes 40–50% of all other collisions silently disappear** — bodies fall through floors and each other, far from the body that caused it, which makes it very hard to debug. (`isOverlapping` treats a `NaN` box as overlapping nothing, so the `NaN` body itself never had contacts to lose.)

## Fix

When filling the scratch arrays, map a `NaN` left edge to `Float.POSITIVE_INFINITY` (e.g. `sweepMinX[i] = if (left.isNaN()) Float.POSITIVE_INFINITY else left`). A `NaN` body then sorts to the end, where the `break` on `sweepMinX[j] >= maxXa` stops every other body's scan before it, which is correct because it overlaps nothing. `maxX` can stay as it is (a `NaN` right edge only makes that body's own row scan to the end, which is the last row anyway once its left edge is `+inf`; a finite-left/`NaN`-right body is correct, just unpruned). No allocation, one comparison per body per sub-step, and for all-finite input the arrays and therefore the pair order and simulation are bit-identical. Keep the comment on the loop to one line on why (the sort cannot order `NaN`).

Add one sentence to the sweep-and-prune paragraph of `plugins/physics/CLAUDE.md`: bodies with `NaN` bounds sort last and never collide.

## Tests

`SweepAndPruneNanTest` in `desktopTest`: zero gravity, 200 pairs of overlapping circles far apart from each other
(pair `k`'s first body at `x = k * 100`, its second at `x = k * 100 + 5`, radius large enough to overlap), plus one
circle at `x = NaN`. The **order of the list matters**: add the 200 first bodies, then the `NaN` body, then the 200
second bodies, in one `add(list)`. The sort starts from the list order and can't move anything across the `NaN`, so
at HEAD it ends as `[first bodies sorted, NaN, second bodies sorted]`: each first body's scan breaks on the next
first body 100 units on, and a second body only scans forward, so only the last pair is found. (Pairs added in
ascending order — a first body directly followed by its partner — are already sorted and pass at HEAD, so they
prove nothing.) Start a manual-tick Kubriko instance and tick 16 ms at a time until **any** body's position changes
(poll, 2 s timeout — the async registration). Assert that after that same tick every pair's two bodies moved
(positions differ from their start). At HEAD 199 of the 200 pairs are untouched; after the fix all move.

Run `./gradlew :plugins:physics:desktopTest`.

## Manual check

None — no Showcase scene produces a `NaN` body.
