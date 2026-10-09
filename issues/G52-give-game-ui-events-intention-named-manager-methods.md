# Move each game's UI-event handling and click sounds out of the entry Composables into intention-named manager methods.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGame.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/WallbreakerGame.kt`, each game's `implementation/managers/UIManager.kt`, `implementation/managers/AudioManager.kt`, `implementation/managers/UserPreferencesManager.kt`, the `navigateBack` in each `*GameStateHolderImpl.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/GameplayHud.kt` (after G15)

## Problem
Click-sound ownership is split three ways, so each new button has to remember where its sound goes:
- In the entry Composables: `onInfoButtonPressed = { stateHolder.audioManager.playClickSoundEffect(); stateHolder.uiManager.toggleInfoDialogVisibility() }` (`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/WallbreakerGame.kt:111–114`, and the same pattern in every game for info, exit, close-confirmed, fullscreen and level selection).
- Inside managers: Wallbreaker's `fun toggleInfoDialogVisibility() = _isInfoDialogVisible.update { !it.also { if (it) audioManager.playClickSoundEffect() } }` and `fun toggleCloseConfirmationDialogVisibility() = _isCloseConfirmationDialogVisible.update { !it.also { audioManager.playClickSoundEffect() } }` (`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/managers/UIManager.kt:77–79`); `UserPreferencesManager.onAreSoundEffectsEnabledChanged()` plays it too.
- As a result Wallbreaker's exit button queues the click twice (once in the lambda, once in `toggleCloseConfirmationDialogVisibility`); it is only heard once because `AudioManager` collects queued URIs into a `Set` per tick (`soundUrisToPlay`, `AudioManager.kt:36,66`). A side effect inside `MutableStateFlow.update {}` also runs again if the update retries.

## Fix
Per game, give the UI manager intention-named entry points that own both the state change and the sound: `onInfoButtonPressed()`, `onInfoDialogClosed()`, `onExitButtonPressed()`, `onCloseCancelled()`, `onCloseConfirmed()` (emits `backNavigationIntent` via a callback the state holder passes in), `onFullscreenToggled(onFullscreenModeToggled)`. The entry Composables pass method references. `navigateBack` (or G51's action mapping) calls the same methods. Sound plays outside `update {}`.

## Decision
- Where the click sound lives: **in the UI manager methods (recommended)**, vs in `AudioManager` wrappers, vs in the Composables (today's majority). Settle together with G50 option B if that is taken.
- Whether to keep Wallbreaker's per-tick dedup as the guard against double queuing (recommended: yes, but stop relying on it).

## Behaviour
Same sounds at the same moments (one click per press), same state transitions. Order of "play sound" vs "change state" within one press is kept per call site.

## Public API
None.

## Tests
None beyond G51's (sounds need a `SoundManager`; state toggles are trivial).

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop :examples:game-blockys-journey:compileKotlinDesktop :examples:game-space-squadron:compileKotlinDesktop :examples:game-wallbreaker:compileKotlinDesktop`

## Manual check
In every game, press each menu button and dialog button, and system back on Android: exactly one click per press, none missing.
