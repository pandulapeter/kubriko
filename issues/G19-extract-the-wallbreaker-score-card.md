# Extract the duplicated Wallbreaker score card into a private `ScoreCard`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/GameOverlay.kt`

## Problem
`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/GameOverlay.kt:82–103` writes the same `WallbreakerCard { Text(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), text = …, color = MaterialTheme.colorScheme.primary, textAlign = …) }` twice, differing only in text and `TextAlign.Start` / `TextAlign.End`.

## Fix
Add `@Composable private fun ScoreCard(text: String, textAlign: TextAlign) = WallbreakerCard { Text(...) }` in the same file (the `Text` verbatim, using the parameters) and call it twice: `ScoreCard(text = stringResource(Res.string.score, score), textAlign = TextAlign.Start)` and `ScoreCard(text = stringResource(Res.string.highscore, highScore), textAlign = TextAlign.End)`.

## Behaviour
Same tree: each call emits one `WallbreakerCard` as before, inside the same `Row`.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-wallbreaker:compileKotlinDesktop`

## Manual check
none
