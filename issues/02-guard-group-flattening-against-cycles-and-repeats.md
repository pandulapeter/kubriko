# Guard Group flattening against cycles and repeated members

**Challenged:** amended — the visited set compares with `equals`, which *is* new for additions (two distinct but equal actors in one `add` are now collapsed), so the plan says so and the KDoc names it.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Group.kt` (KDoc), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/GroupFlatteningTest.kt` (new), `engine/CLAUDE.md` (Actor Batch Processing)

## Problem

`ActorManagerImpl.flattenActors` (0008d027, ~404-419) only refuses a group that lists *itself*:

```kotlin
private fun flattenActors(initialActors: List<Actor>): List<Actor> {
    val result = ArrayList<Actor>()
    val queue = ArrayDeque(initialActors)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        result.add(current)
        if (current is Group) {
            for (child in current.actors) {
                if (child !== current) {
                    queue.addLast(child)
                }
            }
        }
    }
    return result
}
```

Two groups that contain each other (`g1.actors = [g2]`, `g2.actors = [g1]`) loop forever. The live stress run observed it: the processor hung, heap grew to 1.6 GB, and every later `add`/`remove` of that Kubriko instance was never processed (the processor is a single coroutine). A diamond (two groups sharing a child, or one group listing a child twice) emits the child twice, which the next plan's duplicate check would then have to clean up. `engine/CLAUDE.md` already claims the BFS is "cycle-guarded".

## Fix

Track visited actors in `flattenActors`: a `HashSet<Actor>` (created per call — this is the batch path, not the tick path) checked before `result.add(current)`; skip an actor already visited, and only enqueue children not yet visited. This subsumes the `child !== current` check (remove it). The output keeps BFS order with each actor at its first occurrence.

`HashSet` uses `equals`/`hashCode`, the membership notion `processBatch`'s `workingSet` already uses — but at 0008d027 that set only decides *removals*; for additions this is new: two distinct actors that are `equal` (a consumer's `data class` actor, or one overriding `equals`) passed in one `add` now collapse to the first. No in-repo or Tesselar actor overrides `equals` (checked), and Kotlin common has no identity set, so keep `HashSet` and document it rather than work around it.

Add to the `Group` KDoc: "Nested groups are flattened; an actor reachable through several groups (or through a cycle of groups) is added once. Actors are compared with `equals`, so actors should not override it."

`engine/CLAUDE.md` → *Actor Batch Processing*: "flattens `Group` actors (BFS, cycle-guarded)" becomes "(BFS over a visited set, so cycles and shared members are handled)".

## Tests

`GroupFlatteningTest` (desktopTest, uses `ActorTestHarness` from plan 01):
- `cyclicGroupsAreAddedOnce` — `MutableGroup` class with `var actors: List<Actor>`; `g1.actors = listOf(g2, a)`, `g2.actors = listOf(g1, b)`; `add(g1)`; `awaitCondition(2_000) { allActors.value.size == 4 }`; assert the set is exactly `{g1, g2, a, b}`; then `add(c)` is still processed (the processor did not hang).
- `sharedChildIsAddedOnce` — `g1 = [shared]`, `g2 = [shared]`, `add(g1, g2)`; assert `allActors.value.count { it === shared } == 1` and `shared.added == 1`.

## Manual check

None.
