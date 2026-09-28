# Keep Annoyed Penguins' game viewport composed while a level loads

**Challenged:** written during the challenge pass. It was split out of plan 13, whose change would otherwise pause the game on every level load.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (slow loads on the web and Android make it likely)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`

Apply after plans 92 and 93, which edit the same composable. It depends on plan 13 (lane A, merged earlier).

## Problem

Annoyed Penguins wraps its game viewport in `AnimatedVisibility(visible = !isLoadingLevel, enter = fadeIn(), exit = fadeOut())`, so a level load that outlasts the fade-out (the load has a fixed `delay(300)` plus a resource read; the web and Android are the slow cases) removes the viewport from composition. With plan 13 that reports the game instance unfocused, and the game's `UIManager.onInitialize` reacts with `stateManager.isFocused.filterNot { it }.onEach { stateManager.updateIsRunning(false) }` — picking a level would pause the game and show the menu again once the level appears (and `AudioManager` would pause the music for the load). The fix keeps the viewport composed and folds the fade into its existing alpha:

```kotlin
val loadingAlpha by animateFloatAsState(targetValue = if (isLoadingLevel) 0f else 1f)
val gameAlpha by animateFloatAsState(targetValue = if (isGameRunning) 1f else 0.5f, animationSpec = tween())
KubrikoViewport(
    modifier = Modifier.alpha(loadingAlpha * gameAlpha * stateHolder.gameplayManager.gameViewportAlpha.collectAsState().value),
    kubriko = kubriko, // plan 92's local; `stateHolder.kubriko.value` if 92 has not landed
    windowInsets = windowInsets,
)
```

The other in-repo viewports only ever appear once per state holder (`isGameLoaded` in Space Squadron, Wallbreaker and Blocky's Journey; `isReadyToRender` in the isometric demo) or are disposed with their instance, so nothing else changes. Tesselar keeps both of its viewports composed for the whole game (its pause menu opens on `logicStateManager.isFocused == false`, which is what it wants when the viewport goes).

## Fix

As above: remove the `AnimatedVisibility(visible = !isLoadingLevel, ...)` around the game's `KubrikoViewport` and fold an animated `loadingAlpha` into the viewport's existing alpha modifier. Re-locate the code by the `AnimatedVisibility(visible = !isLoadingLevel` snippet, since plans 92 and 93 have changed the surrounding lines. Keep the loading indicator and every other child exactly as it is. Drop imports that become unused.

## Tests

None. This is Compose UI in an example, and examples have no test source sets.

## Manual check

Desktop and web Showcase → Annoyed Penguins: pick each level from the menu. Each time, the level fades in running, the menu does not come back, and the music keeps playing. The fade-in and fade-out still look the same as before.
