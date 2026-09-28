# Stop updating and drawing actors once their removal has been applied

**Decision needed:** should removal callbacks additionally be held back until the tick loop has provably dropped the actor (closing the remaining race with a tick already in progress)? — recommended: no; publish the derived actor lists synchronously and refresh them before the update loop, which removes the deterministic extra update, and document the narrow concurrent case.

**Challenged:** amended — the pre-loop visible draw-cache rebuild is dropped (the existing post-loop rebuild already runs before that frame's draw once the lists are published synchronously, so it only doubled the cull on every churn tick) and the pre-loop dynamic refresh replaces rather than repeats that tick's post-loop cull; the pre-loop block is laid out so plan 07 can add its no-viewport branch there; the KDoc no longer promises no `draw()` for a frame whose tick ran before `onRemoved()`; and it takes over the first-tick determinism test from plan 05.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt` (KDoc of `onRemoved`), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Disposable.kt` (KDoc), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/RemovedActorUpdateTest.kt` (new), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ActorProcessingStartTest.kt` (from plan 05; add one test), `engine/CLAUDE.md` (Tick Dispatch, Draw-Cache Invalidation, Actor Batch Processing)

Apply after plans 01-05.

## Problem

A removed `Dynamic` actor receives at least one more `update()` **after** its `dispose()` and `onRemoved()` ran — the live stress run saw it deterministically in 30 of 30 runs with a manual tick source. Two lags stack:

1. The lists the tick loop reads are derived from `_allActors` through a hop to the main thread (0008d027 ~104-124):
   ```kotlin
   private val dynamicActors by autoInitializingLazy {
       _allActors
           .map { actors -> actors.filterIsInstance<Dynamic>().toImmutableList() }
           .distinctUntilChanged()
           .flowOn(Dispatchers.Default)
           .asStateFlowOnMainThread(persistentListOf())
   }
   ```
   (`visibleActors` and `overlayActors` likewise), while `processBatch` publishes `_allActors` and then runs `dispose()`/`onRemoved()` right away on the processor thread. For some time after `onRemoved()`, `dynamicActors.value` still contains the actor.
2. `onUpdate` iterates `activeDynamicMirror` **before** refreshing `activeDynamicActors` from `dynamicActors` (~322-333 vs ~373-386), so even a fresh `dynamicActors` value only takes effect a tick later.

The draw path has the same shape: `sortedVisibleActorsByLayer` is rebuilt in `onUpdate` from `visibleActors`, so a removed `Visible` is drawn for another frame or more after `dispose()` — a disposed actor drawing a released bitmap or reading cleared state.

## Fix

1. **Derive the per-trait lists inside `processBatch`**, not through flows. Replace the `dynamicActors`, `visibleActors` and `overlayActors` flow chains with private `MutableStateFlow`s (or `@Volatile` fields) that `processBatch` sets right **before** `_allActors.value = ...` (so anyone who sees the new `allActors` also sees the new derived lists) and therefore before any removal callback: one pass over `workingList` filling three `ArrayList`s, then `toImmutableList()` each, publishing only the ones whose content changed (batch path — allocation here is fine). Keep the `shouldComposeLayers == false` short-circuit for `overlayActors`. `layerIndices` stays a flow on the main thread (Compose reads it).
2. **Refresh the active set before iterating.** In `onUpdate`, add a block *above* the update loop, outside the `shouldUpdateActorsWhileNotRunning || isRunning` condition (it is bookkeeping, not updating), that runs only when the dynamic list reference changed since the last refresh:
   ```
   if (dynamicActors !== lastDynamicActors) {
       if (!shouldPutFarAwayActorsToSleep) -> publish the dynamic list as active directly (and set lastDynamicActors)
       else if (viewportSize is not empty) -> updateActiveDynamicActors(camera, scale); remember that the cull ran this tick
       // plan 07 adds its "viewport has no size" branch here
   }
   ```
   Read `viewportSize`, `cameraPosition` and the scale factor for it before the loop (the post-loop code keeps reading its own snapshot after the loop, since updates move the camera). The existing sleeping-off `else if` after the loop becomes redundant and goes. After the loop, keep the post-loop cull for the size-changed and throttle-elapsed triggers, but skip it on a tick where the pre-loop cull already ran — a game that adds or removes something almost every tick (bullets, particles) must not pay two culls per tick.
   **Do not move the visible draw-cache rebuild.** With step 1 the new `visibleActors` reference is published before `onRemoved()`, so the post-loop rebuild of the very next tick (triggered by `visibleActorsList !== lastVisibleActors`) already drops the actor before that frame's draw. Rebuilding it before the loop as well would only cull twice on every tick the list changes.
3. Keep every per-frame path allocation-free (the cull still uses the scratch buffers; nothing new is allocated per tick).

Result: once `onRemoved()` has run, the next tick neither updates nor culls the actor, and the frame drawn after that tick doesn't draw it. A frame drawn between `onRemoved()` and the next tick (the batch landed between a tick and its draw, or ticks are gated while draws still happen) can still draw it once from the previous cache. What remains is inherent to the threading: a batch applied *while* a tick is running (background tick source, or the processor racing the main thread) can call `onRemoved()` while that tick's `update()` of the same actor is still executing or about to.

**Alternative (full guarantee):** hold each batch's removal callbacks until the tick thread has observed the new list (e.g. the tick publishes a "last observed batch generation" and the processor suspends on it before calling `dispose()`/`onRemoved()`, falling back to immediately when no tick is running). This changes the documented "just after the actual removal" timing and needs a fallback for a stopped tick source — hence not the default.

KDoc: `Actor.onRemoved` and `Disposable.dispose` gain: "The actor receives no further `update()` from ticks that start after this is called, and is not drawn in frames rendered after such a tick. A frame already prepared before that tick may still draw it once, so `draw()` must not fail on state released here." Update `engine/CLAUDE.md`: *Tick Dispatch* (active set is refreshed before the update loop), *Draw-Cache Invalidation* (derived lists are published by the batch processor, not through a main-thread hop), *Actor Batch Processing* (order: `onAdded` → publish the derived lists, then `allActors` → `dispose`/`onRemoved`).

## Tests

`RemovedActorUpdateTest` (desktopTest, `ActorTestHarness`, sized viewport):
- `noUpdateAfterOnRemoved` — `CountingActor` that records `updates` and sets a flag in `onRemoved`; add it, await presence, `tick(16)` until `updates >= 1`; `remove(it)`; await the flag; record `updates`; `repeat(5) { tick(16) }`; assert `updates` unchanged. Repeat the whole scenario 50 times in the test to cover timing.
- Same with `shouldPutFarAwayActorsToSleep = false`.
- `newActorIsUpdatedOnTheFirstTickAfterOnAdded` — after `awaitCondition { a in allActors }`, one `tick(16)` gives `a.updates == 1`.
- In plan 05's `ActorProcessingStartTest`, add `firstTickAfterStartUpdatesInitialActors` (moved here from plan 05, because it needs this plan's refresh): a `Dynamic` initial actor counts its `update()` calls; `ActorManager.newInstance(initialActors = listOf(actor), shouldPutFarAwayActorsToSleep = false)` so it does not depend on plan 07; `start()`, one `tick(16)`: assert the count is `1`. Repeat 200 times with fresh instances; every run must pass. This is the determinism check for the documented `start(); tick(); tick()` flow.

## Manual check

Desktop Showcase: play Space Squadron and Wallbreaker for a minute (heavy add/remove churn of bullets, bricks, particles) with the debug menu open; no crash and no flicker of destroyed objects.
