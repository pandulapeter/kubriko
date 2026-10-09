# Use `LoadingIndicator` from `tools/ui-components` for the AnnoyedPenguinsGame loading spinner.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`

## Problem
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt:197–200` hand-builds the loading spinner:
```kotlin
CircularProgressIndicator(
    modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
    strokeWidth = 3.dp,
)
```
`tools/ui-components` already has exactly this component, `LoadingIndicator(modifier: Modifier = Modifier) = CircularProgressIndicator(modifier = modifier.size(24.dp), strokeWidth = 3.dp)` (`tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/LoadingIndicator.kt`), and the module already depends on `projects.tools.uiComponents`. All four games repeat the same three lines.

## Fix
Run after G05 and G15. Replace the call with `LoadingIndicator(modifier = Modifier.align(Alignment.BottomStart))`, import `com.pandulapeter.kubriko.uiComponents.LoadingIndicator`, and drop the now unused `androidx.compose.material3.CircularProgressIndicator` and `androidx.compose.foundation.layout.size` imports (`dp` is still used by `padding(16.dp)`).

## Behaviour
Identical: the resulting modifier chain is `align(BottomStart).size(24.dp)` in the same order, the stroke width is the same 3 dp, and the colours are `CircularProgressIndicator`'s defaults in both cases, read from the same ambient game `MaterialTheme` (`LoadingIndicator` sets no theme or colour of its own). It sits in the same `Box`.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
none
