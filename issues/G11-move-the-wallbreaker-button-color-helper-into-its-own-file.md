# Move `createButtonColor` out of `WallbreakerTheme.kt` into `WallbreakerButtonColor.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/WallbreakerTheme.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/WallbreakerButtonColor.kt` (new)

## Problem
`internal fun createButtonColor(hue: Float) = Color.hsv(hue * 360, 0.3f, 1f)` (`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/WallbreakerTheme.kt:70`) is not part of the theme: the theme never calls it, while `MenuOverlay.kt`, `GameOverlay.kt`, `InfoDialogOverlay.kt` and `CloseConfirmationDialogOverlay.kt` do.

## Fix
- Create `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/WallbreakerButtonColor.kt` (MPL-2.0 header, package `com.pandulapeter.kubriko.gameWallbreaker.implementation.ui`) and move `createButtonColor` into it verbatim, still `internal`, with `import androidx.compose.ui.graphics.Color`.
- The theme keeps its `Color` import (it uses `Color(...)` and `Color.Black`).
- Callers are in the same package; no import changes.

## Behaviour
Verbatim move within one package.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-wallbreaker:compileKotlinDesktop`

## Manual check
none
