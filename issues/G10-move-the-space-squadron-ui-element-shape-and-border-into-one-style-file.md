# Move `SpaceSquadronUIElementShape` and `Modifier.spaceSquadronUIElementBorder()` into `SpaceSquadronUIElementStyle.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/SpaceSquadronTheme.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/Modifiers.kt` (deleted), `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/SpaceSquadronUIElementStyle.kt` (new)

## Problem
- `internal val SpaceSquadronUIElementShape: CornerBasedShape = RoundedCornerShape(` sits in `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/SpaceSquadronTheme.kt:64` but is used by `CloseConfirmationDialog.kt`, `InfoDialog.kt`, `managers/UIManager.kt` and by the border modifier.
- `internal fun Modifier.spaceSquadronUIElementBorder() = border(` is alone in a generically named `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/Modifiers.kt:18` (the code style forbids catch-all file names). The two always go together: the border is drawn with the shape.

## Fix
- Create `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/SpaceSquadronUIElementStyle.kt` (MPL-2.0 header, package `com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui`) holding, verbatim and still `internal`: `SpaceSquadronUIElementShape` (moved from the theme) and `Modifier.spaceSquadronUIElementBorder()` (moved from `Modifiers.kt`), with their imports.
- Delete `Modifiers.kt` (`git rm`).
- Trim the theme's imports (`CornerBasedShape`, `CornerSize`, `RoundedCornerShape`, and `dp` if nothing else uses it).
- The package is unchanged, so `managers/UIManager.kt`'s imports `...implementation.ui.SpaceSquadronUIElementShape` / `...implementation.ui.spaceSquadronUIElementBorder` still resolve.
- Grep the repo for `Modifiers.kt` and `SpaceSquadronTheme.kt` in docs/`CLAUDE.md` and fix references.

## Behaviour
Verbatim move within one package.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-space-squadron:compileKotlinDesktop`

## Manual check
none
