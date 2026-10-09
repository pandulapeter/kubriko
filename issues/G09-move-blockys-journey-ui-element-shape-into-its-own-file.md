# Move `BlockysJourneyUIElementShape` out of `BlockysJourneyTheme.kt` into its own file.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/BlockysJourneyTheme.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/BlockysJourneyUIElementShape.kt` (new)

## Problem
`internal val BlockysJourneyUIElementShape: CornerBasedShape = RoundedCornerShape(` (`examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/BlockysJourneyTheme.kt:64`) is a building block used outside the theme by `UnfinishedDisclaimer.kt` (:33, :37) and `CloseConfirmationDialog.kt` (:48, :52), so other files import it out of the theme file.

## Fix
- Create `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/BlockysJourneyUIElementShape.kt` (MPL-2.0 header copied from a sibling, package `com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui`) and move the `BlockysJourneyUIElementShape` declaration into it verbatim (still `internal`), with its imports (`CornerBasedShape`, `CornerSize`, `RoundedCornerShape`, `dp`).
- Trim imports the theme no longer uses (check `dp` before removing it).
- The users are in the same package, so they need no import change.
- Grep the repo for `BlockysJourneyUIElementShape` in docs/`CLAUDE.md` and update any file reference.

## Behaviour
Verbatim move within one package; the same `val` is used by the theme's `Shapes(...)` and the two dialogs.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-blockys-journey:compileKotlinDesktop`

## Manual check
none
