# Drop the obsolete "zero-sized viewport puts actors to sleep" workaround from the test fixtures' KDoc and from tests that do not need it

**Challenged:** sound — zero-sized viewport publishes every Dynamic actor as active regardless of the flag (`ActorManagerImpl.onUpdate`), physics bodies are not `Dynamic` and are read from `allActors`, the steady-state allocation test is unaffected. Shares `PhysicsContractTest.kt` with 02 (different hunks): land 02 first or in the same lane.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all (tests run on desktop only)
**Files:**
- `tools/test-fixtures/src/desktopMain/kotlin/com/pandulapeter/kubriko/testFixtures/ManualKubriko.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ActorProcessingStartTest.kt`
- `plugins/physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/physics/SweepAndPruneNanTest.kt`
- `plugins/physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/physics/PhysicsAccumulatorTest.kt`
- `plugins/physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/physics/PhysicsContractTest.kt`
- (checked, no change: `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/DisposeDuringTickTest.kt`, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/RemovedActorUpdateTest.kt`, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/performance/HotPathAllocationTest.kt`, `tools/test-fixtures/README.md`, `CLAUDE.md`, `engine/CLAUDE.md`)

## Problem

`newManualKubriko`'s KDoc (`ManualKubriko.kt:66-73` at 76992502) still describes a limitation that commit 0a58175d ("Keep Dynamic actors awake while the viewport has no size.") removed:

```kotlin
/**
 * Creates a [ManualKubriko] with the provided [managers] and a [TickSource.manual], started unless [shouldStart] is
 * false.
 *
 * Without a `KubrikoViewport` the viewport has no size. Until the engine stops putting actors of a zero-sized
 * viewport to sleep, a test that relies on `Dynamic` updates must pass
 * `ActorManager.newInstance(shouldPutFarAwayActorsToSleep = false)`. Engine tests that need a sized viewport use the
 * engine's own `newTestKubriko`.
 */
```

The engine no longer does that. `ActorManagerImpl.onUpdate` publishes every Dynamic actor as active while the viewport is empty:

```kotlin
} else if (!viewportManager.size.value.isEmpty()) {
    updateActiveDynamicActors(viewportManager.cameraPosition.value, viewportManager.currentScaleFactor())
    didCullDynamicActorsBeforeUpdate = true
} else {
    // Without a measured viewport nothing is far away; the first measured size triggers a real cull.
    lastDynamicActors = currentDynamicActors
    lastViewportSizeForDynamic = null
    ...
```

The later cull (`if (shouldPutFarAwayActorsToSleep && !didCullDynamicActorsBeforeUpdate)`) is also guarded by `!viewportSize.isEmpty()`. `ActorManager.kt` KDoc ("Has no effect while the viewport has no size…"), root `CLAUDE.md` ("not applied while the viewport has no size") and `engine/CLAUDE.md` were updated in that commit, and `HeadlessActorUpdateTest` covers the behavior (`dynamicActorsUpdateWithoutAViewport`, `documentedManualExampleUpdatesOnTheFirstTick`, `sleepStartsOnceAViewportIsMeasured`). Only the fixture KDoc is stale. `tools/test-fixtures/README.md` doesn't mention it.

Every test that passes `shouldPutFarAwayActorsToSleep` gets a verdict below:

| File | Uses | Verdict |
|---|---|---|
| `engine/.../ActorProcessingStartTest.kt` `firstTickAfterStartUpdatesInitialActors` | Bare `Kubriko.newInstance` with no viewport and `ActorManager.newInstance(initialActors = listOf(actor), shouldPutFarAwayActorsToSleep = false)`, then asserts `assertEquals(1, actor.updates.get())` | **Remove the flag.** It was the headless workaround. The assertion is positive, so a regression would fail loudly and the test still proves the same thing (initial actors get updated on the first tick after `start()`). Note that it then matches `HeadlessActorUpdateTest.documentedManualExampleUpdatesOnTheFirstTick`. Keep both, because each one lives with the behavior it guards (start-up processing vs headless culling). |
| `plugins/physics/.../SweepAndPruneNanTest.kt`, `PhysicsAccumulatorTest.kt`, `PhysicsContractTest.kt` (`withPhysics`) | `newManualKubriko(ActorManager.newInstance(shouldComposeLayers = false, shouldPutFarAwayActorsToSleep = false), PhysicsManager…)` | **Remove the flag, keep `shouldComposeLayers = false`.** `TestBall`/`TestBody` implement only `RigidBody : Collidable`, not `Dynamic`, and `PhysicsManagerImpl` reads bodies from `actorManager.allActors`, not `activeDynamicActors`. So the flag never affected these tests, and they prove the same thing without it. |
| `engine/.../DisposeDuringTickTest.kt` `disposingFromAManagerEndsTheTick` | Bare headless instance with `ActorManager.newInstance(shouldPutFarAwayActorsToSleep = false)`, asserts `assertEquals(0, actor.updates.get())` | **Keep.** The assertion is negative. The flag guarantees that the actor would otherwise be updated, so the test can't pass vacuously if headless culling ever regressed. Without the flag it would still pass today, but it would depend on a behavior that another test owns. |
| `engine/.../RemovedActorUpdateTest.kt` `noUpdateAfterOnRemovedWithoutSleeping` | `newTestKubriko` (sized 1920×1080 viewport) with the flag `false` | **Keep.** It is a deliberate variant that covers the non-sleeping code path next to `noUpdateAfterOnRemoved`, not a workaround. |
| `engine/.../performance/HotPathAllocationTest.kt` | `shouldPutFarAwayActorsToSleep = true` on a sized viewport | **Keep.** Explicit, not a workaround. |
| `examples/game-space-squadron`, `examples/game-annoyed-penguins` (code + their `CLAUDE.md`) | Game configuration, with documented gameplay reasons | **Keep.** Not tests. |

## Fix

1. In `ManualKubriko.kt`, replace the second KDoc paragraph of `newManualKubriko` with a statement of the current behavior. For example:

   ```kotlin
   /**
    * Creates a [ManualKubriko] with the provided [managers] and a [TickSource.manual], started unless [shouldStart] is
    * false.
    *
    * Without a `KubrikoViewport` the viewport has no size: every `Dynamic` actor is updated and
    * `visibleActorsWithinViewport` stays empty. Engine tests that need a sized viewport use the engine's own
    * `newTestKubriko`.
    */
   ```

2. In `ActorProcessingStartTest.firstTickAfterStartUpdatesInitialActors`, change

   ```kotlin
   ActorManager.newInstance(
       initialActors = listOf(actor),
       shouldPutFarAwayActorsToSleep = false,
   ),
   ```
   to `ActorManager.newInstance(initialActors = listOf(actor)),`.

3. In `SweepAndPruneNanTest`, `PhysicsAccumulatorTest` and `PhysicsContractTest.withPhysics`, change
   `ActorManager.newInstance(shouldComposeLayers = false, shouldPutFarAwayActorsToSleep = false)` to
   `ActorManager.newInstance(shouldComposeLayers = false)`.

4. Don't touch the files marked **Keep** above. No README or CLAUDE.md change is needed, because they already describe the new behavior.

Follow the `code-style` skill (KDoc wording, no fixed-bug archaeology in the new KDoc).

## Tests

```bash
./gradlew :engine:desktopTest --tests "com.pandulapeter.kubriko.ActorProcessingStartTest"
./gradlew :plugins:physics:desktopTest
./gradlew :tools:test-fixtures:compileKotlinDesktop
```
All must pass. As a final check, `grep -rn "shouldPutFarAwayActorsToSleep = false" --include='*.kt' engine plugins tools` should list only `DisposeDuringTickTest.kt` and `RemovedActorUpdateTest.kt`.

## Manual check

None
