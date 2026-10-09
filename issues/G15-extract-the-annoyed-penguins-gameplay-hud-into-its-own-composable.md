# Extract the Annoyed Penguins in-game top bar into `GameplayHud`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/GameplayHud.kt` (new)

## Problem
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt` (:110–140) builds the in-game top bar inline — `Row(modifier = Modifier.windowInsetsPadding(windowInsets).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp))` holding the pause `AnnoyedPenguinsButton`, `ZoomSlider(modifier = Modifier.weight(1f), …)` and `StarCounter` — three layout levels deep inside the entry Composable, next to the menu and the loading indicator.

## Fix
Run after G05.
- Create `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/GameplayHud.kt` (MPL-2.0 header, package `com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui`) with
  `@Composable internal fun GameplayHud(windowInsets: WindowInsets, onPauseButtonPressed: () -> Unit, onButtonHover: () -> Unit, minimumScaleFactor: Float, maximumScaleFactor: Float, currentScaleFactor: Float, onScaleFactorChanged: (Float) -> Unit, collectedStarCount: Int, totalStarCount: Int)` whose body is the `Row` verbatim, with `AnnoyedPenguinsButton(onButtonPressed = onPauseButtonPressed, icon = Res.drawable.ic_pause, title = stringResource(Res.string.pause), onPointerEnter = onButtonHover)`, `ZoomSlider(modifier = Modifier.weight(1f), …, updateScaleFactor = onScaleFactorChanged)` (the `weight` stays inside, where the `RowScope` is) and `StarCounter(collectedStarCount, totalStarCount)`.
- In `AnnoyedPenguinsGame.kt`, the `AnimatedVisibility(visible = isGameRunning && !isLoadingLevel, …)` content becomes a single `GameplayHud(...)` call, passing the existing lambdas (`{ stateHolder.audioManager.playButtonToggleSoundEffect(); stateHolder.stateManager.updateIsRunning(false) }`, `stateHolder.audioManager::playButtonHoverSoundEffect`, `{ stateHolder.gameplayManager.onScaleFactorChanged(); stateHolder.viewportManager.setScaleFactor(it) }`) and the collected values (`stateHolder.viewportManager.rawScaleFactor.collectAsState().value.vertical`, `…collectedStarCount…`, `…totalStarCount…`) unchanged.
- Move the `ic_pause` / `pause` / `stringResource` / `ZoomSlider` / `StarCounter` / `Row` / `Arrangement` imports to the new file; trim what `AnnoyedPenguinsGame.kt` no longer uses.

## Behaviour
Same tree: the `Row` is still the `AnimatedVisibility`'s only child, with the same modifier; the slide animation measures the same content. Values are still collected in the `AnimatedVisibility` content scope.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
Start a level: the pause button, zoom slider (filling the middle) and star counter look and behave as before.
