# Make TickSource start and stop safe across threads and never run two tick loops at once

**Challenged:** amended — the `initializeInternal()` guard is now specified (a skip-if-busy flag let a second concurrent `start()` begin ticking before the first had finished initializing the Managers and applying plan 05's queued actors; a blocking guard would deadlock on re-entrant `start()`), `KubrikoImpl.kt` is added to the files, and the timing-coupled 150–220 ticks/s bound is replaced by a leak check that does not depend on timer resolution.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TickSource.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoImpl.kt`, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/TickSourceLifecycleTest.kt` (new), `documentation/TICK_SOURCE.md` (Lifecycle, Extending TickSource), `CLAUDE.md` (`TickSource` section)

## Problem

`TickSource.start()`/`stop()` (0008d027 ~90-124) are check-then-act on a plain flag:

```kotlin
fun start() {
    kubrikoImpl.initializeInternal()
    if (!isRunning) {
        ...
        isRunning = true
        onStart()
```

and the coroutine-based sources replace their job without cancelling or waiting for the previous one (~245-258, ~271-295):

```kotlin
override fun onStart() {
    job = scope.launch {
        emitTick(0)
        while (isActive) { delay(intervalInMilliseconds); emitTick(intervalInMilliseconds.toInt()) }
    }
}
override fun onStop() {
    job?.cancel()
    job = null
}
```

- Racing `start()`/`stop()` from several threads (a game pausing from a background coroutine while the UI resumes) runs `onStart()` twice or `onStop()` between another thread's flag write and its `onStart()`. The live stress run leaked 7 tick loops and measured 1605 ticks/s instead of 200; the parallel loops called `onTick` concurrently and `ActorManagerImpl`'s tick-thread-private scratch buffers threw `IndexOutOfBoundsException`.
- Even on one thread, `stop(); start()` cancels the old job without waiting: if the old loop is inside `emitTick` on `Dispatchers.Default` at that moment, the new loop's `emitTick(0)` overlaps it.
- The KDoc says calling `start()`/`stop()` "multiple times is safe" but nothing about threads; a custom `TickSource` has no stated rule about calling `emitTick` concurrently.

## Fix

1. **Serialize lifecycle transitions without blocking.** Keep `_isRunning` as the *requested* state and set it with `compareAndSet` in `start()`/`stop()` (return early when it didn't change). Apply transitions through a private drain loop guarded by an `AtomicBoolean` `isApplyingTransition` (`kotlin.concurrent.atomics`, opt in to `ExperimentalAtomicApi` if needed): whoever wins the flag calls `onStart()`/`onStop()` until the applied state (a private `var` only touched while holding the flag) equals `_isRunning.value`, releases the flag, and re-checks once more so a request that arrived while it was releasing is not lost. `onStart()` and `onStop()` are then never called concurrently or out of order, and a start/stop pair collapses without leaking. `kubrikoImpl.initializeInternal()` stays first in `start()`.
   **Initialization guard (`KubrikoImpl`).** Two threads calling `start()` at once both run `initializeInternal()`, and at 0008d027 both can pass `if (!isInitialized)` and run every Manager's `onInitialize` twice (`Manager.initializeInternal` is check-then-act too) — and after plan 05, drain the actor queue twice. Guard it with an `AtomicInt` (or `AtomicBoolean`) claimed by `compareAndSet`, so exactly one caller initializes. The loser must **not** simply return and carry on into `onStart()`: its ticks would then reach Managers the winner has not initialized yet and a scene plan 05 has not applied yet. It must not block waiting for the winner either: a re-entrant `start()` from inside initialization (a Manager's `onInitialize` or a pre-start `onAdded` during plan 05's drain calling `start()`) would wait on itself. Instead make ticks wait for initialization: keep `isInitialized` as a `@Volatile` flag set only at the very end of the winner's `initializeInternal()` (after plan 05's drain), and in `KubrikoImpl.onTick` return early while it is false, next to plan 09's `isDisposed` check (one more volatile read per tick, no allocation). `dispose()` keeps resetting it. The winner's own `start()` then applies its transition normally; a loser's `start()` returns before initialization has finished, which is only observable by a thread that raced another `start()`.
2. **Hand over between loops.** In `FixedRateTickSource` and `FixedFrequencyTickSource`: in `onStop()` cancel the job but keep the reference; in `onStart()` capture `val previous = job` and launch the new loop with `previous?.cancelAndJoin()` as its first statement, before `emitTick(0)`. Store `job` only from inside the (now serialized) `onStart()`.
3. **State the contract in KDoc:** `start()`/`stop()` — "Safe to call from any thread; concurrent calls are applied in order and `onStart()`/`onStop()` never overlap." `emitTick` — "Must not be called concurrently with itself: ticks drive non-thread-safe engine state. A source should emit from one thread or coroutine at a time." `onStart`/`onStop` — "Never called concurrently."

No per-tick cost: the atomics are only touched on start/stop.

Docs: `documentation/TICK_SOURCE.md` → *Lifecycle* (thread safety of `start()`/`stop()`) and *Extending TickSource* (the `emitTick` rule); root `CLAUDE.md` → the TickSource lifecycle paragraph ("Calling `start()`/`stop()` multiple times is safe" gains "from any thread").

## Tests

`TickSourceLifecycleTest` (desktopTest):
- `concurrentStartStopLeavesOneLoop` — a `fixedRate(5)` source on a Kubriko with a `CountingManager` whose `onUpdate` also records the maximum number of threads inside it simultaneously (`AtomicInteger` enter/exit); 8 threads each run 1000 random `start()`/`stop()` calls; finish with `start()`; sleep 500 ms; assert max concurrency == 1. Then `stop()`, sleep 100 ms, record the tick count, sleep another 300 ms and assert it did not change — a leaked loop keeps ticking after `stop()` (at 0008d027 `onStop()` only cancels the last job it knows of), which this detects without depending on the timer resolution a ticks-per-second bound would (a `delay(5)` loop runs at ~64/s on Windows).
- `concurrentStartsInitializeOnce` — a Manager counting `onInitialize` calls and an initial actor counting `onAdded` calls; 8 threads released by one `CountDownLatch` each call `start()` once; assert both counts are `1`, and that every tick the Manager's `onUpdate` observed came after its `onInitialize` (record a flag).
- `stopThenStartDoesNotOverlapTicks` — a manager whose `onUpdate` sleeps 20 ms and records concurrency; `start()`, then 200 × (`stop(); start()`) on the test thread; assert max concurrency == 1.
- Same two tests for `fixedFrequency(200)`.

## Manual check

None.
