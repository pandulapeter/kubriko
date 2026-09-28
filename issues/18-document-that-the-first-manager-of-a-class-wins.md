# Document that the first Manager of a class passed to newInstance wins

**Challenged:** sound

**Decision needed:** both `CLAUDE.md` files say the *last* Manager of a type wins, the code keeps the *first*. Fix the docs or the code? — recommended: fix the docs (changing the code would silently swap which Manager a game gets).

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `engine` (KDoc only)
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt` (KDoc of `newInstance`), `CLAUDE.md` (the `Kubriko` section), `engine/CLAUDE.md` (Gotchas), `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/ManagerDeduplicationTest.kt` (new)

## Problem

`KubrikoImpl` (0008d027 ~38-39) deduplicates with `distinctBy`, which keeps the first element of each key:

```kotlin
val managers: List<Manager> = buildList {
    addAll(manager.distinctBy { it::class })
```

The live stress run confirmed the first instance wins. The docs say otherwise:
- `CLAUDE.md`: "The internal Manager set is deduplicated by type; the last instance added wins."
- `engine/CLAUDE.md` → Gotchas: "Default Managers are prepended; user-supplied same-type Manager wins via last-wins deduplication".

Two related facts are right and must stay: a user-supplied built-in Manager replaces the default one (defaults are only added when `none { it is ActorManager }` etc.), and deduplication is by exact class (`it::class`), while `get()` matches by `isInstance`, so two different subclasses of one abstract Manager type are both kept and `get()` returns the first registered.

## Fix

**Recommended (docs):**
- `Kubriko.newInstance` KDoc, `@param manager`: "If several Managers of the same class are passed, only the first one is used. A default Manager is only created for a built-in type that none of them provides."
- `CLAUDE.md`: "…deduplicated by class; the first instance of a class wins. Default Managers …"
- `engine/CLAUDE.md` Gotchas: "Default Managers are prepended only for built-in types no user Manager provides; among user Managers of the same class the first one wins (`distinctBy`)."

**Alternative (code):** keep the last instance (`manager.reversed().distinctBy { it::class }.reversed()`) and leave the docs. Observable for any caller that passes duplicates.

## Tests

commonTest, `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/ManagerDeduplicationTest.kt` (new) — pins the documented behaviour:
- `firstManagerOfAClassWins` — `val a = M(); val b = M()`; `Kubriko.newInstance(a, b, tickSource = TickSource.manual())`; `assertSame(a, kubriko.get<M>())`.
- `userBuiltInManagerReplacesTheDefault` — pass `ViewportManager.newInstance(initialScaleFactor = 3f)`; assert `get<ViewportManager>()` is that instance.

## Manual check

None.
