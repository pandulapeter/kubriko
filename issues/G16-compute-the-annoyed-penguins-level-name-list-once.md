# Compute the Annoyed Penguins level-name list once instead of on every recomposition.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/GameplayManager.kt`

## Problem
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt:150` passes `allLevels = GameplayManager.AllLevels.keys.toImmutableList()` to `MenuOverlay`, building a new list from a constant map every time the menu recomposes.

## Fix
- In `GameplayManager`'s `companion object` (`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/GameplayManager.kt:130–137`), after `val AllLevels = persistentMapOf(...)`, add `val LevelNames = AllLevels.keys.toImmutableList()` (declared after `AllLevels`, so the initialisation order is right; import `kotlinx.collections.immutable.toImmutableList`).
- At the call site pass `allLevels = GameplayManager.LevelNames`; drop the `toImmutableList` import from `AnnoyedPenguinsGame.kt`.

This is a stopgap; G56 (level type with string resources) may replace both. Run after G15.

## Behaviour
Same list contents and order (`persistentMapOf` keeps insertion order). `MenuOverlay` receives an equal list; it now also receives the same instance, which only lets Compose skip more.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
none
