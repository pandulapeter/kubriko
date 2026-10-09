# Extract the Space Squadron HUD (`ProgressBar`, `ShipStatusBars`, `ScoreIndicator`) out of `UIManager` into the `ui` package.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/managers/UIManager.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/ProgressBar.kt` (new), `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/ShipStatusBars.kt` (new), `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/ScoreIndicator.kt` (new)

## Problem
`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/managers/UIManager.kt` mixes input/state logic with rendering: `override fun Composable(windowInsets: WindowInsets)` (:108–166) builds the whole HUD inline, and `private fun ProgressBar(` (:168–189) is a `@Composable` member of the Manager. The visual pieces have clear standalone names and nest four layout levels deep.

## Fix
Run after G10 (the shape/border move). All new files: MPL-2.0 header, package `com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui`, `internal`.
- `ui/ProgressBar.kt`: `internal fun ProgressBar(value: Float, minColor: Color, maxColor: Color)` — the member's body verbatim (`Box(fillMaxWidth().spaceSquadronUIElementBorder()) { animateFloatAsState …; Box(...) }`).
- `ui/ShipStatusBars.kt`: `internal fun ShipStatusBars(healthFraction: Float, multiShootFraction: Float, modifier: Modifier = Modifier) = Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) { ProgressBar(value = healthFraction, minColor = Color.Red, maxColor = Color.Magenta); ProgressBar(value = multiShootFraction, minColor = Color.Red, maxColor = Color.Cyan) }` — the Column of :124–140.
- `ui/ScoreIndicator.kt`: `internal fun ScoreIndicator(highScore: Int, score: Int)` — the `Text(` of :149–164 verbatim (its modifier chain `padding(16.dp).background(…, SpaceSquadronUIElementShape).spaceSquadronUIElementBorder().padding(8.dp, 4.dp)`, `labelSmall`, white, `stringResource(Res.string.score, highScore, score)`).
- `UIManager.Composable` keeps the outer `Box(fillMaxSize().windowInsetsPadding(windowInsets))`, both `AnimatedVisibility` blocks with their `visible` expressions, and the inner `Box(fillMaxSize().padding(16.dp))`. Inside them it calls:
  - `ShipStatusBars(modifier = Modifier.fillMaxWidth(0.5f).align(Alignment.BottomEnd), healthFraction = shipHealth.collectAsState().value / Ship.MAX_HEALTH.toFloat(), multiShootFraction = multiShoot.collectAsState().value / Ship.MAX_MULTI_SHOOT.toFloat())` — `align` needs the `BoxScope`, so it is applied at the call site;
  - `ScoreIndicator(highScore = scoreManager.highScore.collectAsState().value, score = scoreManager.score.collectAsState().value)`.
- Delete the private `ProgressBar` member; trim `UIManager.kt`'s imports (`Column`, `Arrangement`, `height`, `clip`, `lerp`, `MaterialTheme`, `Text`, `background`, `animateFloatAsState`, `tween`, `Res`, `score`, `stringResource`, `SpaceSquadronUIElementShape`, `spaceSquadronUIElementBorder` — keep whatever is still used).

## Behaviour
Same layout tree: no wrapper is added or dropped (the Column was the Box's only child and stays so; the Text was the AnimatedVisibility's only child). State is collected in the same composition scope (the `AnimatedVisibility` content lambda — `Column` and `Box` are inline), so recomposition is unchanged.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-space-squadron:compileKotlinDesktop`

## Manual check
Start Space Squadron: the two status bars (bottom right, half width) and the score badge (bottom left) look and animate as before.
