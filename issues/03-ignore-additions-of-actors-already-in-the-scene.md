# Ignore additions of actors that are already in the scene

**Challenged:** amended — the update-count assertion now measures one tick after the actor is already active (at this plan's commit plan 06 has not landed, so the first tick after `allActors` fills updates nothing and the test as written failed with the fix in place), and the KDoc states that "already present" means `equals`.

**Decision needed:** a child shared by two Groups is added once; removing either Group removes it. Keep that and document it, or reference-count shared children? — recommended: document it (reference counting would change what `remove(group)` means for every consumer).

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManager.kt` (KDoc of `add`), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Group.kt` (KDoc), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Unique.kt` (KDoc), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/DuplicateAdditionTest.kt` (new), `engine/CLAUDE.md` (Actor Batch Processing), `CLAUDE.md` (ActorManager bullet list)

Apply after plan 02 (flattening already yields each actor once per operation).

## Problem

`processBatch`'s `Operation.Add` branch (0008d027, ~436-474) never checks membership before appending:

```kotlin
if (newActors.isNotEmpty()) {
    workingList.addAll(newActors)
    workingSet.addAll(newActors)
    didChange = true
    newActors.forEach {
        newlyAdded.add(it)
        newlyRemoved.remove(it)
    }
}
```

Adding an actor that is already in the scene (a second `add(a)`, `add(a, a)`, or two Groups sharing a child) puts it in `allActors` again. The live stress run saw up to 10 copies of one actor, `update()` called several times per tick, and then one `remove(a)` removing every copy (`workingList.removeAll(removalSet)`) while `onRemoved` ran once. The copies are also culled, drawn and collided several times.

Re-adding the **same** `Unique` instance goes through the replacement scan first:

```kotlin
if (actor::class in uniqueTypesToReplace) {
    iterator.remove()
    workingSet.remove(actor)
    newlyRemoved.add(actor)
    newlyAdded.remove(actor)
```

so the instance is "replaced by itself": removed, re-added, `onAdded` fires a second time and `onRemoved` never (stress run: `onAdded = 2`, `onRemoved = 0`).

## Fix

In the `Operation.Add` branch:

1. Build `newActors` as today, then filter it: skip any actor for which `it in workingSet` is already true, and add the survivors to `workingSet` as you go (so a repeat inside one operation is also skipped). Only the survivors go into `workingList` and the callback bookkeeping; if none survive, the operation changes nothing (`didChange` untouched).
2. In the `Unique` replacement scan, do not evict an actor that *is* the incoming instance: skip when `actor === latestUniqueByClass[actor::class]`. The incoming instance is then filtered out by step 1 (already present), so re-adding a live `Unique` is a no-op.

KDoc:
- `ActorManager.add` (both overloads): "Adding an actor that is already in the scene has no effect." — followed by "(Membership uses `equals`: an actor equal to one in the scene is treated as already present.)" This is new for additions: a consumer's `data class` actor equal to one already present would previously have been added, now it is dropped. No in-repo or Tesselar actor overrides `equals`.
- `Group`: "A child shared by several groups is added once; removing any of those groups removes it." (**Alternative**, if the decision goes to reference counting: keep a per-child count of containing groups in `processBatch`, only remove a group child when its count drops to zero, and document that instead. Not recommended — it changes `remove(group)` semantics and needs a map maintained across batches.)
- `Unique`: "Adding the instance that is already in the scene has no effect."

`engine/CLAUDE.md` → *Actor Batch Processing*: note that additions of actors already present are dropped. Root `CLAUDE.md` → the `ActorManager` `add(...)` bullet: append "adding an actor that is already present is a no-op".

## Tests

`DuplicateAdditionTest` (desktopTest, `ActorTestHarness` from plan 01):
- `addingTheSameActorTwiceKeepsOneCopy` — `add(a)`; await; `add(a)`; `add(a, a)`; add a sentinel `s` last and await `s in allActors`; assert `allActors.count { it === a } == 1` and `a.added == 1`. Then tick until the actor is being updated (`awaitCondition { tickSource.tick(16); a.updates.get() > 0 }` — before plan 06 the derived lists reach the tick loop through a main-thread hop and the loop iterates the previous tick's active set, so the first ticks may update nothing), record `before = a.updates.get()`, `tick(16)` once more and assert `a.updates.get() - before == 1` (it is 4 at 0008d027: four copies).
- `removingAfterDuplicateAddsRemovesOnce` — continue from above: `remove(a)`; await absence; assert `a.removed == 1`.
- `reAddingTheLiveUniqueInstanceIsANoOp` — `CountingUnique : CountingActor(), Unique`; `add(u)`; await; `add(u)`; add sentinel and await; assert `u.added == 1`, `u.removed == 0`, `u in allActors`.
- `replacingAUniqueWithAnotherInstanceStillWorks` — `add(u1)`, await, `add(u2)` of the same class; await `u2 in allActors && u1 !in allActors`; assert `u1.removed == 1`, `u2.added == 1`.

## Manual check

None.
