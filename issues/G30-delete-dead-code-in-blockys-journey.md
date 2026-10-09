# Delete the commented-out back-navigation branch and the unused counter-clockwise direction in Blocky's Journey.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/BlockysJourneyGameStateHolderImpl.kt` (after G02; `BlockysJourneyGameStateHolder.kt` before it), `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/actors/Blocky.kt`

## Problem
- `navigateBack` keeps a commented-out branch (`BlockysJourneyGameStateHolder.kt:196–198` at 2480325f):
  ```kotlin
  //            } else if (!uiManager.isCloseConfirmationDialogVisible.value) {
  //                audioManager.playButtonToggleSoundEffect()
  //                stateManager.updateIsRunning(true)
  ```
- `private val Direction.nextDirectionCounterClockwise` (`examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/actors/Blocky.kt:140–150`) is never read (grep of the repo and `../Tesselar`: only its declaration; `Blocky` only uses `nextDirectionClockwise`, :64).

## Fix
Run after G02. Delete the three commented lines and the whole `nextDirectionCounterClockwise` property.

## Behaviour
Dead code only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-blockys-journey:compileKotlinDesktop`

## Manual check
none
