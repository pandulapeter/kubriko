# Stop the current tick when the Kubriko instance is disposed during it

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/DisposeDuringTickTest.kt` (new)

## Problem

A game that ends itself from game logic — `kubriko.dispose()` called from an actor's `update()` or a Manager's `onUpdate()` (e.g. "game over → leave") — disposes every Manager while `KubrikoImpl.onTick` is still iterating them (0008d027 ~154-158):

```kotlin
internal fun onTick(deltaTimeInMilliseconds: Int) {
    for (i in managersForTick.indices) {
        managersForTick[i].onUpdateInternal(deltaTimeInMilliseconds)
    }
}
```

`ActorManagerImpl.onUpdate` keeps calling `update()` on the remaining active actors, then every later Manager in the array gets `onUpdate` after its own `onDispose` ran. Anything they touch that checks disposal throws (`kubriko.get()` throws "Cannot access Managers on a disposed Kubriko instance"); the live stress run saw the exception escape `tick()` and the disposed managers keep ticking. `isDisposed` is also a plain `var` written by whichever thread disposes.

## Fix

1. In `KubrikoImpl`, annotate `isDisposed` with `@kotlin.concurrent.Volatile` and expose it as `internal val isDisposedInternal get() = isDisposed` (or make the backing property `internal` with a private setter).
2. In `onTick`, return as soon as the instance is disposed: check `if (isDisposed) return` before the loop and at the top of each iteration.
3. In `ActorManagerImpl.onUpdate`, break out of the `activeDynamicMirror` loop when `kubrikoImpl.isDisposedInternal` becomes true, and return before the cull/draw-cache work.

Two volatile reads per Manager per tick and one per actor; no allocation.

## Tests

`DisposeDuringTickTest` (desktopTest, `ActorTestHarness`):
- `disposingFromUpdateEndsTheTick` — a `CountingManager` registered after the ActorManager counts `onUpdate` calls; an actor whose `update()` calls `kubriko.dispose()`; add it plus a second counting actor; `tick(16)` must not throw; assert the counting manager's `onUpdate` count did not increase on that tick and a further `tick(16)` is a no-op (the tick source is stopped).
- `disposingFromAManagerEndsTheTick` — same with a Manager earlier in the array calling `dispose()` in its `onUpdate`.

## Manual check

None.
