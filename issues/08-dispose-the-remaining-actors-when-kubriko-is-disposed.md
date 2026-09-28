# Dispose the remaining actors when the Kubriko instance is disposed

**Challenged:** sound

**Decision needed:** `kubriko.dispose()` never calls `Disposable.dispose()` or `onRemoved()` for the actors still in the scene. What should it call? — recommended: `Disposable.dispose()` only (resource cleanup), not `onRemoved()` (game logic such as `Ship.onRemoved() = gameplayManager.onGameOver()` in Space Squadron must not fire on teardown).

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Disposable.kt` (KDoc), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt` (KDoc of `dispose`), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/KubrikoDisposeActorsTest.kt` (new), `CLAUDE.md` (`Kubriko` bullets, `Disposable` row), `engine/CLAUDE.md` (Actor Batch Processing)

Apply after plans 01-07.

## Problem

`KubrikoImpl.dispose()` (0008d027 ~124-137) stops the tick source, calls `onDisposeInternal()` on every Manager and cancels the scope. `ActorManagerImpl` does not override `onDispose`, and cancelling the scope kills the batch processor, so actors still in `allActors` never get `Disposable.dispose()` (the stress run: 0 of 10). `Disposable`'s whole purpose is releasing resources ("Should be implemented by Actors that need to perform cleanup when removed from the scene"), and the normal way a game ends is `kubriko.dispose()`, not removing every actor first — so anything an actor holds (native audio handles, bitmaps, listeners registered with a shared Manager, coroutines on another scope) leaks on every game exit.

## Fix

Override `onDispose()` in `ActorManagerImpl`:

1. Mark the manager as disposing with a `kotlin.concurrent.atomics.AtomicBoolean` (opt in to `ExperimentalAtomicApi` if the compiler still requires it) and cancel the processor's `Job` (store it when launching; plan 05 moved the launch into `startProcessingOperations()`).
2. **Recommended:** for every actor in `_allActors.value` that `is Disposable`, call `dispose()` on the calling thread (the thread that called `kubriko.dispose()`), each in its own `try/catch` like plan 01 (log; do not rethrow — the scope is going away). Do not call `onRemoved()`. Do not clear `_allActors` (the published snapshot stays valid for anyone holding it).
   **Alternative A:** also call `onRemoved()` after each `dispose()` — matches the removal contract exactly but fires game logic during teardown.
   **Alternative B:** call nothing and document that actors must be removed before disposing to get their callbacks.
3. In `processBatch`, check the disposing flag after publishing and skip the removal callbacks of a batch that raced with `dispose()` if the flag is set; queued but unprocessed operations are dropped. The remaining window (a batch publishing between the flag set and the snapshot read) is acceptable and is not worth a lock.

This runs once per `dispose()`, not per frame.

KDoc:
- `Disposable.dispose`: "Also called, without a following `onRemoved()`, for every actor still in the scene when the owning [Kubriko] instance is disposed — on the thread that called `Kubriko.dispose()`."
- `Kubriko.dispose`: "Actors still in the scene receive `Disposable.dispose()`."

Docs: root `CLAUDE.md` → the `kubriko.dispose()` bullet and the `Disposable` table row; `engine/CLAUDE.md` → *Actor Batch Processing* (one sentence on teardown).

## Tests

`KubrikoDisposeActorsTest` (desktopTest, `ActorTestHarness`):
- `disposeReachesLiveDisposableActors` — add 10 `CountingActor`s (they implement `Disposable`), await all present, `kubriko.dispose()`; assert every `disposed == 1` and every `removed == 0` (recommended option).
- `disposeIsCalledOnce` — an actor removed before `kubriko.dispose()` keeps `disposed == 1` after it.
- `aThrowingDisposeDoesNotStopTheOthers` — one actor throws from `dispose()`; the others are still disposed and `kubriko.dispose()` returns normally.

## Manual check

Desktop Showcase: open and close Annoyed Penguins and Space Squadron several times with the debug menu's log visible; no errors on close, and music/sounds stop.
