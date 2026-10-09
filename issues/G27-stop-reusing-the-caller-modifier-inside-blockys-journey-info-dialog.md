# Stop applying the caller's `modifier` to the inner text of the Blocky's Journey `InfoDialog`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/InfoDialog.kt`

## Problem
`examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/InfoDialog.kt` applies `modifier` to the root `Box(modifier = modifier.fillMaxSize())` (:38) and again to the inner `Text(modifier = modifier.align(Alignment.Center).verticalScroll(...)…)` (:41), so anything a caller passes would be applied twice.

## Fix
The inner `Text` starts its chain from `Modifier.align(Alignment.Center)`.

## Behaviour
Unchanged today: the only caller (`examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/MenuOverlay.kt:85`) passes no `modifier`, so the `Text` got `Modifier` either way.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-blockys-journey:compileKotlinDesktop`

## Manual check
none
