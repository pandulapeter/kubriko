# Pair actor lifecycle callbacks when one batch both adds and removes the same actor

**Challenged:** sound

**Decision needed:** an actor added and removed within one batch currently gets `dispose()`/`onRemoved()` without ever getting `onAdded()`; one removed and re-added gets a second `onAdded()` without `onRemoved()`. Which callbacks should they get? — recommended: added-then-removed gets the full `onAdded` → `dispose` → `onRemoved` sequence; removed-then-re-added gets none (it simply stays).

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManager.kt` (KDoc), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt` (KDoc), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/BatchCallbackPairingTest.kt` (new), `engine/CLAUDE.md` (Actor Batch Processing)

Apply after plans 01-03.

## Problem

`processBatch` (0008d027) keeps two sets and lets each operation cancel the other's entry:

```kotlin
// Operation.Add
newActors.forEach {
    newlyAdded.add(it)
    newlyRemoved.remove(it)
}
// Operation.Remove
validRemovals.forEach {
    newlyRemoved.add(it)
    newlyAdded.remove(it)
}
// Operation.RemoveAll
workingList.forEach {
    newlyRemoved.add(it)
    newlyAdded.remove(it)
}
```

- `add(x)` then `remove(x)` landing in one batch: `x` leaves `newlyAdded`, so `onAdded` never runs, but `dispose()` and `onRemoved()` do. The stress run counted 500 such actors in a churn test. An actor whose `onRemoved` releases something `onAdded` acquired (e.g. unregisters a listener, reads a `lateinit` manager) crashes or corrupts state — and plan 01 now surfaces that crash.
- `remove(x)` then `add(x)` for an actor already in the scene: `x` leaves `newlyRemoved`, so `onRemoved` never runs, but `onAdded` runs a second time on a live actor.

Batches are formed by timing (everything queued while the previous batch ran), so which case a game hits is nondeterministic.

## Fix

Replace the two cancel-out rules in the `Add`, `Remove`, `RemoveAll` branches (and the `Unique` eviction, which is a removal) with:

- **Add of `x`** (after plan 03's membership filter): if `newlyRemoved.remove(x)` returns true, `x` was in the scene when the batch started and is merely staying — schedule nothing. Otherwise `newlyAdded.add(x)`.
- **Removal of `x`** (it is in `workingSet`): `newlyRemoved.add(x)` and leave `newlyAdded` alone. If `x` was added earlier in this batch it now gets `onAdded` (before publishing) *and* `dispose`/`onRemoved` (after), and is absent from the published list.

Walk-through the executor should check against: not-present add→remove ⇒ `onAdded`, `dispose`, `onRemoved`; present remove→add ⇒ nothing; not-present add→remove→add ⇒ `onAdded` only; present remove→add→remove ⇒ `dispose`, `onRemoved`.

**Alternative:** treat both cases as net no-ops (no callbacks for add→remove). Rejected as the default because an actor that acquires something in its constructor and releases it in `Disposable.dispose()` would leak.

KDoc: `ActorManager.add`/`remove`/`removeAll` gain one sentence each: "An actor added and removed before the batch is applied still receives `onAdded()` followed by `onRemoved()`; an actor removed and re-added in one batch stays in the scene without either callback." Mirror it once in `Actor`'s class KDoc. `engine/CLAUDE.md` → *Actor Batch Processing*: one sentence with the same rule.

## Tests

`BatchCallbackPairingTest` (desktopTest; uses the `Blocker` from `ActorTestHarness` to force a single batch):
- `addThenRemoveInOneBatchGetsBothCallbacks` — block the processor, `add(x)`, `remove(x)`, release; await `x.removed.get() == 1`; assert `x.added == 1`, `x.disposed == 1`, `x.removed == 1`, `x !in allActors`.
- `removeThenAddInOneBatchGetsNoCallbacks` — `add(x)`, await; block, `remove(x)`, `add(x)`, add a sentinel `s`, release; await `s in allActors`; assert `x.added == 1`, `x.removed == 0`, `x in allActors`.
- `removeAllThenAddKeepsTheActorWithoutCallbacks` — as above with `removeAll()` then `add(x)`; the blocker's own batch is applied first, so the `removeAll` batch also removes the blocker — await `allActors.value == listOf(x)` and assert `x.added == 1`, `x.removed == 0`.

(The operations issued while the blocker sits in `onAdded` all land in the *next* batch, which is the one under test.)

## Manual check

None.
