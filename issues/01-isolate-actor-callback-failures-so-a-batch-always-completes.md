# Isolate actor callback failures so a batch of actor operations always completes and is published

**Challenged:** amended — the rethrow's per-platform effect is stated accurately (it is not the same path as an `update()` exception under `viewportFrames()`), and the handler assertion now awaits the asynchronous rethrow instead of racing it. Testing extension: sound — the fixtures it imports exist in plan `00`, `newTestKubriko` is the only helper needing `internal` API, and the rethrow goes to the scope's `SupervisorJob`, so it cannot cancel the processor that `awaitProcessed` relies on.

**Extended (testing extension):** the harness is built on `:tools:test-fixtures` from plan `00`; only `newTestKubriko` (which needs `internal` engine API) is still defined here.

**Decision needed:** when an actor's `onAdded`/`dispose`/`onRemoved` throws, should the engine surface it as a crash after finishing the batch, or only log it? — recommended: finish the batch, then rethrow the first failure through the Kubriko scope (it surfaces like an exception thrown from `update()`).

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt` (KDoc), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Disposable.kt` (KDoc), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ActorTestHarness.kt` (new; builds on `:tools:test-fixtures`), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ActorCallbackFailureTest.kt` (new), `engine/CLAUDE.md` (Actor Batch Processing)

## Problem

The batch processor launched in `ActorManagerImpl.onInitialize` (at 0008d027, ~305-318) swallows every exception, including `CancellationException`, and logs nothing:

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
```

`processBatch` runs every callback inline and only publishes at the end:

```kotlin
if (newlyAdded.isNotEmpty()) {
    newlyAdded.forEach { it.onAdded(kubrikoImpl) }
}
if (didChange) {
    _allActors.value = workingList.toImmutableList()
}
if (newlyRemoved.isNotEmpty()) {
    newlyRemoved.forEach {
        (it as? Disposable)?.dispose()
        it.onRemoved()
    }
}
```

One throwing `onAdded` therefore skips every later `onAdded` of the batch, never publishes `_allActors` (so none of the batch's additions *or removals* take effect), and skips every removal callback. The live stress run observed it: a batch of `before, thrower, after` left `before` with `onAdded` called but not in the scene, and `after` lost entirely — silently, with logging disabled or not. A throwing `dispose()`/`onRemoved()` skips the remaining removal callbacks (the list was already published, so those actors are gone but never cleaned up). Games see actors vanish or never appear with no trace.

## Fix

In `ActorManagerImpl`:

1. Wrap each individual callback in its own `try/catch (e: Exception)`, rethrowing `CancellationException` immediately. Record the first caught exception in a local `var firstFailure: Exception? = null` (later ones are logged only).
2. Keep the order: every `onAdded`, then publish `_allActors` (always, when `didChange`), then for each removed actor `dispose()` then `onRemoved()` — a failing `dispose()` must still let that actor's `onRemoved()` run, each in its own `try`.
3. Log every caught failure with `log(message = "Actor callback failed: ...", details = e.stackTraceToString(), importance = Logger.Importance.HIGH)` (the Manager `log` is guarded by `isLoggingEnabled`, which is fine — the rethrow below is what makes it visible by default).
4. **Recommended:** after the batch is fully applied, if `firstFailure != null`, rethrow it outside the processor loop so the loop survives: `scope.launch { throw firstFailure }`. On a `SupervisorJob` scope with no handler this reaches the uncaught-exception handler of a `Dispatchers.Default` thread: Android and iOS terminate the app, the desktop JVM prints the stack trace and keeps running, web logs it to the console. (This is *not* the path of an exception from `Dynamic.update()` under `viewportFrames()` — that one propagates through the viewport's `LaunchedEffect` into Compose and ends the composition on every platform. Only under `fixedRate`/`fixedFrequency`, whose ticks run on the Kubriko scope, do the two behave alike. Say "rethrown on the Kubriko scope" in KDoc, not "like an update() exception".) Capture `firstFailure` into a `val` before the `launch` (a captured `var` does not smart-cast).
   **Alternative (log only):** skip the rethrow; failures are reported only through the log (visible only with `isLoggingEnabled = true`). Choose this only if the user prefers games to keep running with a broken actor.
5. In the processor loop replace `catch (_: Exception) { }` with `catch (e: CancellationException) { throw e } catch (e: Exception) { log(...) }` so an engine-side bug in `processBatch` itself is also no longer silent.

KDoc: add one sentence to `Actor.onAdded`/`Actor.onRemoved` and `Disposable.dispose`: "An exception thrown here does not prevent the rest of the batch from being applied; it is rethrown afterwards on the Kubriko scope." (adjust to "is logged" under the alternative). Update `engine/CLAUDE.md` → *Actor Batch Processing* with one sentence on per-callback isolation.

No per-frame cost: this is the batch path, not the tick path.

## Tests

Create the engine's test harness, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ActorTestHarness.kt` (MPL header). Later actor plans reuse it. `awaitCondition`, `CountingActor`, `Blocker`, `awaitProcessed` and `recordingUncaughtExceptions` already exist in `:tools:test-fixtures` (plan `00`), which every module's `desktopTest` depends on. Import them; do not redefine them. The harness adds only what needs engine internals:
- `fun newTestKubriko(vararg managers: Manager, actorManager: ActorManager = ActorManager.newInstance()): Pair<KubrikoImpl, ManualTickSource>`. It creates the instance with `TickSource.manual()`, calls `start()`, and calls `viewportManager.updateSize(Size(1920f, 1080f))` (internal, but accessible from the module's own tests).

Where a later plan says "await presence" or "await absence" of an actor that the test itself added or removed, use `actorManager.awaitProcessed()` instead of polling. Keep `awaitCondition` for signals that come from another thread, such as the asynchronous rethrow below.

`ActorCallbackFailureTest` (wrap each test in `recordingUncaughtExceptions { recorded -> … }` from the fixtures):
- `throwingOnAddedDoesNotDropTheRestOfTheBatch` — in one batch add `before`, a `thrower` whose `onAdded` throws, `after`; await `allActors.size == 3` (or 2 if the thrower is to be excluded — assert the chosen semantics: recommended keeps the thrower in the list since its addition already happened from the engine's point of view); assert `before.added == 1`, `after.added == 1`, and (recommended option) `awaitCondition { recorded.isNotEmpty() }` — the rethrow is launched after the list is published, so it can arrive after `allActors` already has 3 entries — then assert the handler recorded exactly one `IllegalStateException` from the thrower. Do not run these tests inside `runTest`: `kotlinx-coroutines-test` (plan 00) installs an exception collector that would capture the rethrow before the default handler sees it.
- `throwingDisposeStillCallsOnRemovedAndOtherRemovals` — add three actors, the middle one's `dispose()` throws; remove all three in one call; await `allActors.isEmpty()`; assert every actor's `removed == 1`.
- `processorKeepsWorkingAfterAFailure` — after a throwing batch, a later `add(x)` is still processed (`awaitCondition { x in allActors.value }`).

## Manual check

None beyond the tests.
