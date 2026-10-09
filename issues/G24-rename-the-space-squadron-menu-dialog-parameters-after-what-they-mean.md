# Rename the Space Squadron menu's dialog flags and callbacks after what they mean.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/MenuOverlay.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/CloseConfirmationDialog.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt`
**Challenged:** amended — corrected the two line references in `MenuOverlay.kt` (`CloseConfirmationDialog(onCloseCanceled = …)` is :123, the exit button in `Title` is :209).

## Problem
In `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/MenuOverlay.kt` (renamed from `SpaceSquadronMenuOverlay` by G13):
- `shouldShowInfoText: Boolean` (:63) is whether the info dialog is visible, and `shouldCloseConfirmationDialog: Boolean` (:64) reads as an instruction but is whether the close confirmation dialog is visible.
- `onLeaveButtonPressed: () -> Unit` (:66) is used both to open the dialog (the exit button in `Title`, :209) and to cancel it (`CloseConfirmationDialog(onCloseCanceled = onLeaveButtonPressed, …)`, :123); the caller wires it to `uiManager::toggleCloseConfirmationDialogVisibility`.
- `CloseConfirmationDialog`'s `onCloseCanceled` (`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/CloseConfirmationDialog.kt:35`) is spelled `onCloseCancelled` in the other three games.

## Fix
Run after G13. Rename, in the declarations, every use inside the files, and the named arguments in `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt`:
- `shouldShowInfoText` → `isInfoDialogVisible`
- `shouldCloseConfirmationDialog` → `isCloseConfirmationDialogVisible`
- `onLeaveButtonPressed` → `onCloseConfirmationToggled` (also the parameter of the private `Title`)
- `onCloseCanceled` → `onCloseCancelled`

## Behaviour
Renames of internal/private parameters only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-space-squadron:compileKotlinDesktop`

## Manual check
none
