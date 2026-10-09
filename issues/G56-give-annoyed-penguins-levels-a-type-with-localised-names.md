# Give Annoyed Penguins levels a type with localised display names instead of using "Map 1/2/3" map keys as labels.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/GameplayManager.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/MenuOverlay.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`, new `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/Level.kt` (or in `managers/`), `examples/game-annoyed-penguins/src/commonMain/composeResources/values/strings.xml`, `examples/game-annoyed-penguins/CLAUDE.md`
**Challenged:** amended — covers G17's `LevelSelector` signature and keeps the level list a precomputed constant (what G16 bought).

## Problem
`GameplayManager.AllLevels = persistentMapOf("Map 1" to "level_1.json", "Map 2" to "level_2.json", "Map 3" to "level_3.json")` (`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/GameplayManager.kt:132–136`) uses the on-screen label as the identity of a level: `currentLevel: StateFlow<String?>` holds it, `setCurrentLevel(level: String)` takes it, and `MenuOverlay` shows it directly — `title = if (currentLevel == level) stringResource(Res.string.resume) else level` (`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/MenuOverlay.kt:228`). The label is a hardcoded user-facing string outside `strings.xml`, and renaming it would change the level identity.

## Fix
- `internal enum class Level(val sceneFileName: String) { LEVEL_1("level_1.json"), LEVEL_2("level_2.json"), LEVEL_3("level_3.json"), }`.
- `currentLevel: StateFlow<Level?>`, `setCurrentLevel(level: Level)`, `loadScene(level?.sceneFileName)`; `MenuOverlay(allLevels = …, onLevelSelected: (Level) -> Unit)` and G17's private `LevelSelector(allLevels: ImmutableList<String>, currentLevel: String?, onLevelSelected: (String) -> Unit, …)` switch to `Level` too. Drop `AllLevels`; G16's `LevelNames` becomes a constant `val AllLevels = Level.entries.toImmutableList()` (or similar) so the menu still gets the same list instance on every recomposition instead of a fresh `Level.entries.toImmutableList()`.
- Label: `<string name="level_name">Map %1$d</string>` and `stringResource(Res.string.level_name, level.ordinal + 1)` (or a `StringResource` per entry).

## Decision
- Parameterized `Map %1$d` (recommended: one string, levels stay data) vs one `StringResource` per enum entry (allows per-level names later).
- Keep the visible text "Map 1/2/3" (recommended) or rename.

## Behaviour
Same labels, same levels, same resume logic (`currentLevel == level`). `setCurrentLevel`'s "same level must not freeze physics" guard compares enum entries instead of strings.

## Public API
None.

## Tests
None needed (no logic beyond the enum); optional `desktopTest` that every `Level.sceneFileName` exists under `files/scenes/` is not possible without resource access — skip.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
Menu shows "Map 1/2/3"; selecting the current level shows "Resume" and resumes without freezing physics; finishing a level returns to the menu.
