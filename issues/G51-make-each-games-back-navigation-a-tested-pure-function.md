# Turn each game's `navigateBack` decision tree into a pure, unit-tested function.

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/AnnoyedPenguinsGameStateHolderImpl.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/BlockysJourneyGameStateHolderImpl.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/SpaceSquadronGameStateHolderImpl.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/WallbreakerGameStateHolderImpl.kt` (the Impls were split out by G01–G04, landed in 44ba0a30, 076722b3, 63f2f596, 3b4c454c), new `implementation/BackNavigation.kt` in each game, new `examples/<game>/src/desktopTest/kotlin/com/pandulapeter/kubriko/<gamePackage>/implementation/BackNavigationTest.kt` in each game, each game's `CLAUDE.md`

## Problem
Every game's `navigateBack(isInFullscreenMode, onFullscreenModeToggled)` (`AnnoyedPenguinsGameStateHolderImpl.kt:237–258`, `BlockysJourneyGameStateHolderImpl.kt:182–200`, `SpaceSquadronGameStateHolderImpl.kt:165–183`, `WallbreakerGameStateHolderImpl.kt:150–168`) is an `if/else if` chain over manager state that has drifted between the games, with no test:
- Wallbreaker: `stateManager.isRunning.value` → `gameplayManager.pauseGame()`; info visible → toggle info; `gameplayManager.isGameStarted` → `resumeGame()`; fullscreen → click + exit fullscreen; else toggle close confirmation. It resumes even while the close-confirmation dialog is open (no `!isCloseConfirmationDialogVisible` guard, which Space Squadron and Annoyed Penguins have).
- Space Squadron: running **and not game over** → pause; …; `isGameStarted && !isCloseConfirmationDialogVisible` → `playGame()`.
- Annoyed Penguins: plays the toggle sound in every branch but the last; resume condition is `currentLevel != null && !isCloseConfirmationDialogVisible`.
- Blocky's Journey: no resume branch at all (it used to be commented out; G30 deleted the comment, landed in bb54039a).
Whether a given back press pauses, resumes, closes a dialog or leaves the game is hard to see and easy to break.

## Fix
Per game, in `implementation/BackNavigation.kt`:
- `internal enum class BackNavigationAction { PAUSE, CLOSE_INFO_DIALOG, RESUME, EXIT_FULLSCREEN, TOGGLE_CLOSE_CONFIRMATION, }` (only the entries that game uses; Blocky's Journey has no `RESUME`).
- `internal fun backNavigationAction(isRunning: Boolean, isInfoDialogVisible: Boolean, …game-specific flags…, isInFullscreenMode: Boolean): BackNavigationAction` reproducing today's chain exactly.
- `navigateBack` keeps its `isLoadingDone.also { if (it) … }` shape and maps the action to today's calls (and sounds) with a `when`.
- Tests: one `desktopTest` per game enumerating the truth table, including the drift cases above, so they are pinned as current behaviour.

## Decision
- **Shared or per game?** **Per game (recommended)** — the branches differ on purpose (game over, level loaded). A single function in `examples/shared` would need a flag per difference.
- **Unify the drift?** Out of scope: this plan pins today's behaviour. Whether Wallbreaker should stop resuming behind an open close-confirmation dialog, and whether Blocky's Journey should resume on back, are separate `Kind: bug` decisions to raise once the tests exist.

## Behaviour
Unchanged; the tests pin it.

## Public API
None.

## Tests
`BackNavigationTest` per game: every combination of the inputs → expected action. Pure functions, no Kubriko instance needed (`desktopTest` is the source set; it already depends on `:tools:test-fixtures`).

## Verify
`./gradlew :examples:game-annoyed-penguins:desktopTest :examples:game-blockys-journey:desktopTest :examples:game-space-squadron:desktopTest :examples:game-wallbreaker:desktopTest`

## Manual check
On Android, press system back in each game in each state (playing, paused, info open, close dialog open, fullscreen on desktop via Esc where wired): same result as before.
