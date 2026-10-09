# Let the engine's coroutine context and clocks be injected internally, so tests run on virtual time

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** engine
**Challenged:** amended — the injected clock is a primitive `fun interface` instead of a `TimeSource` (through the interface `markNow()`/`TimeMark.plus` allocate on every tick, which today's inline `ValueTimeMark` loop does not), and tests feed it the scheduler's virtual time rather than an independent `TestTimeSource` (which would not move with virtual `delay`s); step 2 also says where it goes if E50 is not taken.
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoImpl.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt` (only if the internal factory lives there)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/FixedFrequencyTickSource.kt` (created by E01)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/FixedRateTickSource.kt` (created by E01)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TickSource.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorBatchProcessor.kt` (created by E50)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/helpers/TickSourceLifecycleTest.kt` (moved there by E03)
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ActorProcessingStartTest.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ActorTestHarness.kt`
- `engine/CLAUDE.md`

Depends on E01 (tick sources in their own files), E03 (test paths) and E50 (the processor's `launch`).

## Problem

`code-style`: "a long-lived class takes its `CoroutineScope`, dispatcher and clocks rather than building them, so a
test can pass virtual time." At 2480325f the engine builds all three itself:

```kotlin
override val coroutineContext = SupervisorJob() + Dispatchers.Default              // KubrikoImpl.kt:42
processorJob = scope.launch(Dispatchers.Default) {                                // ActorManagerImpl.kt:316 (→ ActorBatchProcessor after E50)
var lastTickTime = TimeSource.Monotonic.markNow()                                 // FixedFrequencyTickSource (TickSource.kt:320, :327)
```

so the timing tests sleep on the wall clock and are slow and flaky by construction:

```kotlin
Thread.sleep(100)          // TickSourceLifecycleTest.kt:91 (also :115, :118, :120, :133)
Thread.sleep(100)          // ActorProcessingStartTest.kt:54
```

## Fix

All new parameters are `internal` with today's values as defaults; `Kubriko.newInstance` and the public `TickSource`
factories keep their signatures and behaviour.

1. `KubrikoImpl` takes `coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default` as an internal
   constructor parameter (or an internal `Kubriko.newTestInstance`-style factory next to `newInstance`; recommended:
   the constructor parameter, since tests already cast `Kubriko.newInstance(...) as KubrikoImpl` and
   `ActorTestHarness` can construct `KubrikoImpl` directly). Keep the `SupervisorJob` semantics when a caller passes
   a dispatcher: build it as `SupervisorJob() + dispatcher` from an internal `dispatcher: CoroutineDispatcher =
   Dispatchers.Default` parameter rather than accepting an arbitrary context — recommended, as it keeps
   "child failures don't cancel the engine" (engine/CLAUDE.md Gotchas) true by construction.
2. `ActorBatchProcessor.start` launches on that same dispatcher (passed in by `ActorManagerImpl` from `KubrikoImpl`)
   instead of the hard-coded `Dispatchers.Default`. If E50 was declined or has not landed, make the same change in
   `ActorManagerImpl.startProcessingOperations` (`scope.launch(Dispatchers.Default)`), which is where the launch then
   still lives.
3. `FixedFrequencyTickSource` takes an injectable clock; `TickSource.fixedFrequency(ticksPerSecond)` keeps passing the
   default. **Do not type it as `TimeSource`:** today's loop calls `TimeSource.Monotonic.markNow()` and
   `nextTickStart += targetInterval` on the inline `ValueTimeMark`, which allocates nothing; through the `TimeSource`
   interface `markNow()` returns a boxed `TimeMark` and `TimeMark.plus` a new object, i.e. allocations on every tick of
   a tick source (root `CLAUDE.md` → Performance forbids that). A `() -> Long` lambda boxes its result the same way.
   Recommended: `internal fun interface TickClock { fun nowInNanoseconds(): Long }` (primitive return, no boxing),
   default `TickClock.Monotonic` reading `start.elapsedNow().inWholeNanoseconds` from a `ValueTimeMark` taken once, and
   the loop kept on `Long` nanoseconds with the same arithmetic (`Duration` is a nanosecond `Long` underneath:
   `remainingTime` → `targetIntervalInNanoseconds - (now - nextTickStart)`, the `delay` argument
   `(remaining / 1_000_000).coerceAtLeast(1L)`, the emitted delta `((now - lastTickTime) / 1_000_000).toInt()`, which
   truncates exactly as `inWholeMilliseconds` does for non-negative values). Tests pass
   `TickClock { testScheduler.currentTime * 1_000_000 }` so the clock follows the scheduler's virtual time.
4. Optional, separate commit: `FixedRateTickSource` and `FixedFrequencyTickSource` share the
   `job`/`loopMutex`/`onStop` scaffolding — an internal abstract `CoroutineLoopTickSource` with an abstract
   `suspend fun runLoop()` would hold it once. Only if it reads simpler; not required by this plan.

Then rewrite the sleeping tests on `kotlinx-coroutines-test` (already on every `commonTest` classpath):
a `StandardTestDispatcher`, advancing virtual time with `advanceTimeBy`/`runCurrent` instead of `Thread.sleep`, and
for `FixedFrequencyTickSource` a `TickClock` that reads the scheduler's own virtual time (`testScheduler.currentTime`).
An independent `kotlin.time.TestTimeSource` does not move when the scheduler advances, so the loop's remaining time
would never shrink and every measured delta would read 0. Keep the assertions; where a test asserts on
cross-thread behaviour that virtual time would hide (e.g. `TickSourceLifecycleTest`'s concurrent start/stop from
several threads), keep it on the real dispatcher and leave it as is.

`Dispatchers.Main` used by `ViewportManagerImpl`'s `stateIn(scope + Dispatchers.Main, …)` and the
`asStateFlowOnMainThread` helpers is out of scope: those flows are exercised through their `SyncStateFlow` getters,
which do not need a dispatcher.

## Behaviour
Unchanged in production: every default is today's value.

## Public API
None. All new parameters are internal; the public `Kubriko.newInstance`, `TickSource.fixedRate`,
`TickSource.fixedFrequency` are untouched.

## Tests
The rewritten `TickSourceLifecycleTest` and `ActorProcessingStartTest` cases, plus a new
`helpers/FixedFrequencyTickSourceTest` that drives the scheduler-backed `TickClock` and checks the measured delta and the re-sync
after falling behind (the behaviour root `CLAUDE.md` documents: "delta = measured elapsed time. Re-syncs if behind.
First tick delta = 0").

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:compileKotlinWasmJs :engine:compileKotlinIosSimulatorArm64 :engine:desktopTest`,
and run `:engine:desktopTest` three times to confirm the rewritten tests are stable.

`engine/CLAUDE.md`: the Gotcha "`Manager.scope` is `KubrikoImpl` cast to `CoroutineScope` (`SupervisorJob +
Dispatchers.Default`)" gains "(the dispatcher is an internal constructor parameter for tests)"; "Actor Batch
Processing" says the processor runs on the engine's dispatcher (`Dispatchers.Default` outside tests). The public KDoc
on `Actor`/`ActorManager` that names the background thread stays true.

## Manual check
none
