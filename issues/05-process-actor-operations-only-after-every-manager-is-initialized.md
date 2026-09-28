# Process actor operations only after every Manager is initialized, and apply the ones queued before start synchronously

**Decision needed:** `initialActors` and actors added right after `Kubriko.newInstance()` currently get `onAdded()` (on `Dispatchers.Default`) before custom and plugin Managers are initialized, and `allActors` fills before `TickSource.start()`. Hold actor processing until `start()` has initialized every Manager? — recommended: yes, and apply everything queued before `start()` synchronously inside `start()`, so the first tick after `start()` always sees the initial scene. **Answered 2026-09-28: yes (recommended); the synchronous first drain was added after review of headless use.**

**Challenged:** amended — the startup drain repeats until the channel is empty (operations that pre-start callbacks issue, like `Slingshot.onAdded` adding its parts, were otherwise left to the background loop, so the first tick saw them or not by timing); the once-guard is set before draining (re-entrant `start()`); `Disposable.kt` and the "callbacks run on `Dispatchers.Default`" paragraph of `engine/CLAUDE.md` are added to the edits; and the first-tick update test moved to plan 06, because before 06 the tick loop iterates the previous tick's active set and the test fails with this fix in place.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManager.kt` (KDoc of `newInstance(initialActors)` and `add`/`remove`/`removeAll`), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt` (KDoc of `onAdded`/`onRemoved`), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Disposable.kt` (KDoc of `dispose`), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TickSource.kt` (KDoc of `start`), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ActorProcessingStartTest.kt` (new), `engine/CLAUDE.md` (Initialization Order, Actor Batch Processing), `CLAUDE.md` (ActorManager bullets, Actors section, TickSource lifecycle), `documentation/TICK_SOURCE.md` (Lifecycle, manual example)

## Problem

`KubrikoImpl`'s `init` initializes the four built-in Managers at construction (`actorManager.initializeInternal(this)` etc., 0008d027 ~73-79). `ActorManagerImpl.onInitialize` immediately launches the batch processor and enqueues the initial actors:

```kotlin
scope.launch(Dispatchers.Default) {
    while (isActive) {
        try {
            val firstOp = operationChannel.receive()
            ...
            processBatch(batch)
        } catch (_: Exception) {
        }
    }
}
add(initialActors)
```

Custom and plugin Managers are only initialized later, in `KubrikoImpl.initializeInternal()`, which `TickSource.start()` calls. So every `initialActors` entry, and anything `add()`ed between `newInstance()` and `start()`, runs `onAdded(kubriko)` on a background thread while e.g. `SpriteManager`, `CollisionManager`, `PhysicsManager` have not run `onInitialize` — their `scope` and `manager<T>()` delegates throw. Concretely: an actor calling `spriteManager.get(resource)` in `onAdded` hits `scope.launch` after `SpriteManagerImpl.get` already wrote `resource -> null` into its cache, so that sprite never loads for the lifetime of the instance (the throw itself was swallowed by the processor until plan 01). Whether it happens depends on whether the processor thread beats the first composition — a race.

**Why simply deferring the processor is not enough.** `documentation/TICK_SOURCE.md` documents deterministic headless use:

```kotlin
tickSource.start()
tickSource.tick(deltaTimeInMilliseconds = 16)
tickSource.tick(deltaTimeInMilliseconds = 16)
```

If processing only *began* inside `start()` on `Dispatchers.Default`, the first `tick()` right after `start()` would usually run against an empty scene, and a replay or simulation would drop a varying number of initial frames. Today this mostly works only because the initial batch has usually been processed during `newInstance()`. The fix must make it guaranteed, not merely likely.

## Fix

Plans 01–04 land before this one and change the processor loop and `processBatch`; locate the code by the snippets above, not by line numbers.

1. **`ActorManagerImpl`**
   - Extract the batch-collecting body into a private helper that takes the first operation and drains the rest with `tryReceive()` into one batch (the existing `receive()` + `tryReceive()` loop), so the background loop and the startup drain share it. Keep plan 01's per-callback failure handling exactly as it is.
   - Remove the `scope.launch(...)` from `onInitialize`. Keep `add(initialActors)` there — it only enqueues into the `UNLIMITED` channel, so order is preserved.
   - Add `internal fun startProcessingOperations()`, guarded by a private `var isProcessingStarted` so it runs once per instance. **Set the flag first**, before draining: an `onAdded` that calls `start()` again re-enters `KubrikoImpl.initializeInternal()` while `isInitialized` is still false, and must not start a nested drain.
     1. **Synchronous drain on the calling thread, until the channel is empty:** `operationChannel.tryReceive().getOrNull()` for the first operation; if there is one, drain the rest the same way and call `processBatch(batch)` directly (no coroutine). Then repeat — a new round with whatever the callbacks of the previous round enqueued (an actor's `onAdded` adding its parts, e.g. Annoyed Penguins' `Slingshot.onAdded` → `actorManager.add(activeFakePenguin, waitingFakePenguin, front)`) — until `tryReceive()` returns nothing. Each round is its own batch, so plan 04's pairing applies per round exactly as it does on the background loop. Without the repeat those follow-up operations would go to the background loop and the first tick would see them or not depending on timing, which is the nondeterminism this plan removes. (A scene whose callbacks enqueue forever would spin here instead of on the background thread; that is equally broken either way.) This is safe because nothing else consumes the channel yet, and `processBatch` is not a suspend function.
     2. Then launch the background loop exactly as before (`scope.launch(Dispatchers.Default) { while (isActive) { ... } }`). An `add()` made concurrently from another thread during step 1 is either in that batch or picked up by the loop — the channel is still consumed by one party at a time, so order is preserved.
2. **`KubrikoImpl.initializeInternal()`** — inside the `if (!isInitialized)` block, after `managers.forEach { it.initializeInternal(this) }` and the auto-start, call `actorManager.startProcessingOperations()` before setting `isInitialized = true`. Because `TickSource.start()` is final and always calls `initializeInternal()` before `onStart()`, this covers every tick source, including custom subclasses; `emitTick()` is already a no-op until `start()`, so no tick can run before the drain.
3. Nothing else changes. `add`/`remove`/`removeAll` keep enqueuing on the caller's thread, and every operation issued **after** `start()` stays asynchronous, processed on the batch thread as today.

**The guarantee this gives:** when `start()` returns, every operation issued before it — `initialActors` included, and every operation their callbacks issued during the drain — has been applied: `allActors` is published and `onAdded` has run with every Manager initialized. Plan 06 publishes the derived lists synchronously inside `processBatch` and refreshes the active set before the update loop, so from plan 06 on the first tick also *updates* them (and from plan 07 on, even without a viewport); at this plan's own commit the first tick still iterates the previous tick's (empty) active set.

**The thread-contract change this makes:** `onAdded`/`onRemoved` for operations queued before `start()` run on the thread that calls `start()` (with the default `viewportFrames()`: the main thread, inside the viewport's first composition), not on the batch thread. The cost is one longer first frame for a very large initial scene (the stress run measured 1k/10k/50k actors added and published in 3/6/21 ms on desktop JVM).

**Alternative (not recommended):** keep eager processing and only document that `onAdded` may run before custom Managers are initialized. Actors would then need to defer Manager use to their first `update()`. The failure is silent and timing-dependent.

Observable changes, which is why this needs a decision:
- `ActorManager.allActors`, and everything derived from it, stays empty until the first `TickSource.start()`.
- Pre-start operations' callbacks move to the `start()` caller's thread.
- Headless callers gain a guarantee: the initial scene is present on the first tick.

KDoc:
- `ActorManager.newInstance(initialActors)`: "Added when the [Kubriko] instance is started (the first `TickSource.start()`), after every Manager has been initialized; they are in [allActors] by the time `start()` returns."
- `ActorManager.add`/`remove`/`removeAll`: "Operations issued before the instance is started are queued and applied synchronously during `start()`, on the thread that calls it. Later operations are applied asynchronously on a background thread."
- `Actor.onAdded` / `Actor.onRemoved`: replace the current threading sentences ("on the background thread that processes the batch … - not the main thread") with: "Every Manager of the instance is initialized by the time this is called. Called on the background batch thread, or — for operations issued before the instance started — on the thread that called `TickSource.start()` (the main thread when `KubrikoViewport` starts it). Either way, anything main-thread-confined must be dispatched explicitly."
- `Disposable.dispose`: "on the same background thread" becomes "on the same thread".
- `TickSource.start`: add "Applies every actor operation queued before the first start before returning."

Docs:
- `engine/CLAUDE.md` → *Initialization Order*: add "At the end of `initializeInternal()` the ActorManager applies the operations queued so far synchronously, on the calling thread — repeating until the callbacks enqueue nothing more — then starts its batch processor. So `onAdded` always sees initialized Managers, and the first tick sees the initial scene."
- `engine/CLAUDE.md` → *Actor Batch Processing*: the paragraph "**All three callbacks run on the processor's own `Dispatchers.Default` coroutine, not the main thread.** …" becomes "**All three callbacks run on the processor's own `Dispatchers.Default` coroutine — except for operations queued before the first `start()`, which run on the thread calling it (the main thread under `viewportFrames()`).** An actor's `onAdded`/`onRemoved`/`dispose` must dispatch anything main-thread-confined itself, and must not assume it is off the main thread either; the public KDoc on `Actor` and `ActorManager` says so too, and must keep saying so if the threading is ever revisited."
- Root `CLAUDE.md`:
  - `ActorManager` → `add(...)` / `remove(...)` bullets: replace "on that same background thread" with "on that same background thread (for operations issued before `start()`: synchronously on the thread calling `start()`)".
  - *Actors* code comment for `onAdded` / `onRemoved`: same clause.
  - *TickSource* → Lifecycle: "`start()` initializes Kubriko if needed, applies queued actor operations, and begins emitting ticks".
- `documentation/TICK_SOURCE.md`:
  - *Lifecycle* → "Calling `start()`" list: add "applies all actor additions and removals queued before it, before returning".
  - Under the `manual()` example: add one sentence — "The initial actors are in the scene before the first `tick()`."

## Tests

`ActorProcessingStartTest` (desktopTest). Use `ActorTestHarness` from plan 01, but construct the instance by hand, because the harness starts it.

- `initialActorsWaitForStart`
  - Set up a `RecordingManager : Manager()` with `var initialized = false`, set in `onInitialize`, and an actor whose `onAdded` records `kubriko.get<RecordingManager>().initialized` into an `AtomicReference<Boolean?>`.
  - `Kubriko.newInstance(ActorManager.newInstance(initialActors = listOf(actor)), recordingManager, tickSource = TickSource.manual())`, then `Thread.sleep(100)`.
  - Assert `allActors.value.isEmpty()` and that the recorded value is still `null`.
  - `tickSource.start()`, then **immediately, with no waiting**, assert `actor in allActors.value` and that the recorded value is `true`.
- `callbackOperationsDuringTheDrainAreAppliedToo` — an initial actor whose `onAdded` calls `kubriko.get<ActorManager>().add(child)`; `start()`; with no waiting, assert `child in allActors.value` and `child.added == 1`. Repeat 200 times with fresh instances (at 0008d027 and without the repeat-until-empty rule this passes only when the background loop happens to win).
- (The first-tick *update* determinism test, `firstTickAfterStartUpdatesInitialActors`, belongs to plan 06: until 06 lands, `onUpdate` iterates the active set of the previous tick, derived through a main-thread hop, so it would fail here with this fix in place.)
- `preStartCallbacksRunOnTheStartingThread` — record `Thread.currentThread()` in `onAdded` of an initial actor, call `start()` from the test thread, and assert they are the same thread.
- `addBeforeStartIsAppliedInOrderAfterStart`
  - Before `start()`: `add(a)`, `remove(a)`, `add(b)`. Then `start()`.
  - With no waiting: assert `b in allActors.value` and `a !in allActors.value`.
- `addAfterStartStaysAsynchronous` — after `start()`, `add(c)`, await `c in allActors`, and assert `onAdded` ran on a thread other than the test thread.

## Manual check

- Launch every Showcase example on desktop (`./gradlew :app:desktop:run`) and confirm each scene appears with its sprites. Processing of the initial scene now happens during the viewport's first composition.
- Open the isometric demo, which has the largest initial scene, and confirm there is no visible hitch on its first frame.
