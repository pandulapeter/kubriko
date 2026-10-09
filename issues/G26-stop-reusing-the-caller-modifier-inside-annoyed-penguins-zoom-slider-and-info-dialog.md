# Stop applying the caller's `modifier` to inner children of the Annoyed Penguins `ZoomSlider` and `InfoDialog`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/ZoomSlider.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/InfoDialog.kt`

## Problem
- `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/ZoomSlider.kt` applies its `modifier` to the `Slider` (`modifier = modifier.height(24.dp)`) and again to the thumb: `thumb = { Spacer(modifier.size(16.dp, 16.dp).hoverable(…).background(…)) }` (:46). The only caller passes `Modifier.weight(1f)` (`AnnoyedPenguinsGame.kt:127`, `GameplayHud.kt` after G15), so the thumb carries a `weight(1f)` too.
- `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/InfoDialog.kt` applies `modifier` to the root `Box(modifier = modifier.fillMaxSize())` (:38) and again to the inner `Text(modifier = modifier.align(Alignment.Center)…)` (:41).

Any positioning or sizing a caller passes would be applied twice.

## Fix
- `ZoomSlider.kt`: the thumb becomes `Spacer(Modifier.size(16.dp, 16.dp).hoverable(interactionSource = interactionSource).background(MaterialTheme.colorScheme.primary, CircleShape))`.
- `InfoDialog.kt`: the inner `Text` starts its chain from `Modifier.align(Alignment.Center)` instead of `modifier.align(...)`.

## Behaviour
Unchanged today, verified:
- Material 3 (`org.jetbrains.compose.material3:material3:1.12.0-alpha03`, `Slider.kt`, `SliderImpl`) wraps the `thumb` slot in its own `Box(modifier = Modifier.layoutId(SliderComponents.THUMB).wrapContentWidth()…) { thumb(state) }`. `weight` only sets `RowColumnParentData`, which a `Box` ignores, so the thumb's `weight(1f)` currently has no effect on its measurement; removing it keeps the thumb 16×16 dp.
- `InfoDialog`'s only caller (`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/MenuOverlay.kt:101`) passes no `modifier`, so the inner `Text` got `Modifier` either way.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
Start a level and drag the zoom slider: the thumb is the same 16 dp circle, and hovering it still shows the hover state.
