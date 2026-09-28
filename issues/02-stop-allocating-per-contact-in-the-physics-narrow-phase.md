# Stop allocating per contact in the physics step by unboxing the arbiter contact, recycling arbiters without a copy, and (if approved) reusing the collision result

**Challenged:** amended — verified bit-identity (same floats, same order; reused result is private to its arbiter and copied out immediately), ~710 B/tick estimate (66 − 16 + 20 × 32) and that Tesselar calls neither API; added removal of the now-unused `Ignore` import and the ordering/re-run note with plan 06.

**Kind:** performance  ·  **Severity:** medium  ·  **Platforms:** all
**Files:** `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/implementation/Arbiter.kt`, `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/PhysicsManagerImpl.kt`, `plugins/physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/physics/PhysicsContractTest.kt`, `plugins/physics/CLAUDE.md`. Step 3 only: `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/CollisionResult.kt`, `plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/extensions/CollisionMaskExtensions.kt`, `plugins/collision/src/commonTest/kotlin/com/pandulapeter/kubriko/collision/CollisionMaskContractTest.kt`, `plugins/collision/CLAUDE.md`

Steps 1–2 ship in `plugin-physics` with no API change. Step 3 also ships in `plugin-collision` and adds public API.

**Decision needed:** Step 3 removes the last per-contact allocation (a `CollisionResult`, 32 B per contact per step). It needs one additive public overload in plugin-collision, because physics is a separate module and cannot reach the collision plugin's `internal` narrow phase. Recommended: **approve Step 3**. Steps 1–2 alone bring the existing test under budget, but a scene of truly resting bodies would still allocate about 6.4 KB per tick. If Step 3 is declined, land Steps 1–2 and skip the second test.

## Problem

`CLAUDE.md` requires zero allocation per frame in hot paths. The physics step allocates for every colliding pair on every sub-step. Plan 64 of the first sweep measured this with `PhysicsContractTest.steadyStateTickAllocationWithContacts` and left the test disabled:

```kotlin
    @Ignore("measured 1 266 B/tick with 200 contacts")
    @Test
    fun steadyStateTickAllocationWithContacts() = withPhysics { kubriko, _ ->
        val floor = box(x = 0f, y = 500f, width = 6_000f, height = 100f, density = 0f)
        val bodies = List(200) { index -> circle(x = (index - 100) * 25f, y = 430f) }
```

A probe at 76992502 reproduced 1 266.3 B/tick. The scene is not actually at rest. The circles use the default `restitution = 0.8f` and bounce in lockstep: 200 contacts on every 10th step and none on the others (20 arbiters per step on average). The same scene with `restitution = 0f` really rests, has 200 contacts every step, and measures **12 066 B/tick**. That is 66 B of contact-free baseline plus 60 B per contact.

Per-section probes on a resting circle-vs-floor arbiter show where the bytes go:

| Source | Bytes |
|---|---|
| `Arbiter.narrowPhaseCheck()` total | 56 per contact |
| of which `collisionResultWith(...)` (a new `CollisionResult`) | 32 per contact |
| of which `contacts[0] = …` (boxing a `SceneOffset` into `Array<SceneOffset>`) | 24 per contact |
| `arbiterPool.addAll(arbiters)` (`ArrayList.addAll` copies via `toArray()`) | 4 per arbiter + 16 per step |
| `Arbiter.solve()`, `Arbiter.penetrationResolution()` | 0 |

The three sources in code:

1. `Arbiter.kt`: `SceneOffset` is a value class, so an `Array<SceneOffset>` stores boxed instances. `contacts[1]` is never read or written.
   ```kotlin
       val contacts = arrayOf(SceneOffset.Zero, SceneOffset.Zero)
   ...
               contacts[0] = collisionResult.contact
   ...
           val contactA = contacts[0] - bodyA.position
           val contactB = contacts[0] - bodyB.position
   ```
2. `PhysicsManagerImpl.step`:
   ```kotlin
           arbiterPool.addAll(arbiters)
           arbiters.clear()
   ```
3. `Arbiter.narrowPhaseCheck` → plugin-collision's public `collisionResultWith`, which calls the narrow phase in `RESULT_OBJECT` mode and constructs a new `CollisionResult` for every overlap (`CollisionMaskExtensions.kt`, `private fun collisionResult(...)`, `else` branch).

## Fix

None of the steps change any arithmetic, and all of them keep the order of arbiters and pool entries. The contact, normal and penetration values are the same floats as today, so simulation results are bit-for-bit unchanged and `identicalRunsAreBitForBitIdentical` keeps passing.

### Step 1: unbox the arbiter contact (`Arbiter.kt`)

`Arbiter` is `internal`. Replace `val contacts = arrayOf(SceneOffset.Zero, SceneOffset.Zero)` with a single field:

```kotlin
    var contact = SceneOffset.Zero
```

Then write `contact = collisionResult.contact` in `narrowPhaseCheck`, and use `val contactA = contact - bodyA.position` / `val contactB = contact - bodyB.position` in `solve`. `grep -rn "contacts" plugins/physics/src` finds no other users.

### Step 2: recycle arbiters without the `toArray()` copy (`PhysicsManagerImpl.kt`)

In `step`, replace `arbiterPool.addAll(arbiters)` with an indexed loop that appends in the same order:

```kotlin
        for (i in arbiters.indices) {
            arbiterPool.add(arbiters[i])
        }
        arbiters.clear()
```

### Step 3 (Decision needed): reuse the arbiter's `CollisionResult`

- `CollisionResult.kt`: turn the three constructor `val`s into `var`s with `internal set` (for example `contact: SceneOffset` as a constructor parameter plus `var contact = contact; internal set`, keeping the KDoc `@property` entries). Consumers still see read-only properties, and the getters' binary signatures do not change.
- `CollisionMaskExtensions.kt`: add a public overload with full KDoc:
  ```kotlin
  /**
   * Allocation-free variant of [collisionResultWith] for callers that query the same pair every frame: when
   * [reusableResult] is not `null` it is overwritten with the new contact details and returned instead of a
   * new [CollisionResult]. Returns `null` (leaving [reusableResult] untouched) when the masks do not overlap.
   */
  fun CollisionMask.collisionResultWith(
      other: CollisionMask,
      shouldSkipAxisAlignedBoundingBoxCheck: Boolean,
      reusableResult: CollisionResult?,
  ): CollisionResult?
  ```
  Implement it by passing `reusableResult` down through `collisionCheck` and the `check*Collision` functions to `collisionResult(...)`, which writes into it when it is not null and constructs a new object otherwise. Pass it as a parameter rather than through another file-level `var`, so concurrent Kubriko instances cannot clobber each other. The existing two-argument overload delegates with `reusableResult = null`, so its behavior does not change.
- `Arbiter.kt`: keep `private var collisionResult: CollisionResult? = null`, call the new overload with `reusableResult = collisionResult`, and store the returned non-null result back. Each pooled arbiter then allocates its result once and reuses it afterwards.
- Document the overload in `plugins/collision/CLAUDE.md` next to the `hasCollisionWith` / `collisionResultWith` notes.

### Expected results (estimated from the per-section probes)

- The existing (bouncing) test scene: about 1 266 → about 710 B/tick after Steps 1–2 (under the 1 024 B budget), and about 70 B/tick after Step 3.
- A resting scene with 200 contacts: 12 066 → about 6 450 B/tick after Steps 1–2, and about 70 B/tick after Step 3.

Update the "Arbiter Pooling" section of `plugins/physics/CLAUDE.md` in one line: the contact is an unboxed field, the pool is refilled with an indexed loop (not `addAll`, which copies), and, after Step 3, each arbiter reuses its `CollisionResult`.

## Tests

In `plugins/physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/physics/PhysicsContractTest.kt`:

1. **Un-ignore** `steadyStateTickAllocationWithContacts`. Replace the `println` with the same assertion the contact-free test uses: `assertTrue(allocatedBytesPerTick <= ALLOCATION_BUDGET_IN_BYTES, "Measured $allocatedBytesPerTick B/tick.")`. It passes after Steps 1–2. Remove `import kotlin.test.Ignore`, which has no other user in the file.
2. **Step 3 only:** add `steadyStateTickAllocationWithRestingContacts`, the same scene with `circle(x = (index - 100) * 25f, y = 430f, restitution = 0f)`. It has 200 contacts on every step and must stay within the same budget. Without Step 3 it measures about 6.4 KB/tick, so add it only together with Step 3.
3. `identicalRunsAreBitForBitIdentical` and the other contract tests must pass unchanged.

If Step 3 lands, add a test in `plugins/collision/src/commonTest/kotlin/com/pandulapeter/kubriko/collision/CollisionMaskContractTest.kt`. Take two overlapping masks and check that the new overload returns the passed `reusableResult` instance with fields equal to those of the two-argument overload's result. For a non-overlapping pair, check that it returns `null` and leaves `reusableResult` untouched.

Run `./gradlew :plugins:physics:desktopTest :plugins:collision:desktopTest`.

**Interaction with plan 06:** plan 06 edits `withPhysics` in the same `PhysicsContractTest.kt` (drops `shouldPutFarAwayActorsToSleep = false`). The hunks do not overlap, so either order merges cleanly, but land 06 first and run this plan's allocation tests on top of it, so the budget assertion is verified under the final fixture configuration.

Out-of-repo callers: `../Tesselar` does not use plugin-physics or `collisionResultWith` (it only mentions `CollisionResult` in a comment), so Step 3 affects nobody outside this repo.

## Manual check

None. This is a pure allocation change with bit-identical simulation results, covered by the allocation and determinism tests.
