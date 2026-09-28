# Cancel the previous Annoyed Penguins level load when the level changes again

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/GameplayManager.kt`, `examples/game-annoyed-penguins/CLAUDE.md`

## Problem

Reviewed at `0008d027`. `GameplayManager.onInitialize` (~line 68) starts an independent coroutine for every level change:

```kotlin
currentLevel
    .onEach { loadScene(AllLevels[it]) }
    .launchIn(scope)
```

```kotlin
private fun loadScene(sceneName: String?) = scope.launch {
    _collectedStarCount.update { 0 }
    _totalStarCount.update { 0 }
    if (sceneName == null) {
        actorManager.removeAll()
        delay(100)
        stateManager.updateIsRunning(false)
        _gameViewportAlpha.update { 1f }
    } else {
        _isLoadingLevel.update { true }
        delay(300) // Gives time for the fade animation to hide the previous level
        viewportManager.setScaleFactor(viewportManager.maximumScaleFactor)
        actorManager.removeAll()
        _gameViewportAlpha.update { 1f }
        viewportManager.setCameraPosition(SceneOffset.Zero)
        try {
            val json = Res.readBytes("files/scenes/$sceneName").decodeToString()
            val newActors = serializationManager.deserializeActors(json)
            actorManager.add(newActors)
            _totalStarCount.update { newActors.filterIsInstance<Star>().count() }
            _isLoadingLevel.update { false }
        } catch (_: MissingResourceException) {
        }
    }
}
```

Two level changes closer together than one load (≥300 ms plus reading the scene file) run concurrently. The level buttons stay clickable while the menu slides out after the first click (`AnimatedVisibility` keeps its content interactive during the exit animation), so a quick second click on another map does it. Because `Res.readBytes` is a suspending read of varying duration, the first job's `actorManager.add(newActors)` can land after the second job's `removeAll()`: both maps' actors end up in the scene (`Slingshot` is `Unique`, stars and blocks are not), `_totalStarCount` holds whichever job finished last, `_isLoadingLevel` is cleared by the first job while the second is still loading, and the "all stars collected" check no longer matches the stars on screen, so the level ends early or never.

## Fix

Let each new value cancel the load in progress. Make `loadScene` a `suspend` function (drop the `= scope.launch { … }` wrapper, keep the body) and collect with `collectLatest`:

```kotlin
override fun onInitialize(kubriko: Kubriko) {
    scope.launch {
        currentLevel.collectLatest { loadScene(AllLevels[it]) }
    }
    ...
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadScene(sceneName: String?) {
    ...unchanged body...
}
```

Cancellation can only strike at the suspension points (`delay`, `Res.readBytes`); whichever point it hits, the next load starts with its own `removeAll()` and resets the star counts and `_isLoadingLevel`, so no partial state survives. Imports: add `kotlinx.coroutines.flow.collectLatest`, keep `launch`; drop `launchIn`/`onEach` only if the `stateManager.isRunning` collector below no longer needs them (it does — keep them).

`examples/game-annoyed-penguins/CLAUDE.md`, the `GameplayManager` paragraph (~line 46): add "A level change cancels a load still in progress (`collectLatest`)."

## Tests

None: examples have no test source sets; `GameplayManager` needs a running `Kubriko` with physics, serialization and resources.

## Manual check

Desktop Showcase, Annoyed Penguins: on the level menu, click "Map 1" and, while the menu is still sliding out, click "Map 2" (a double-click sweep across the two buttons). Only Map 2's layout appears, the star counter shows Map 2's total, and collecting all its stars ends the level. Repeat the other way round and with Map 3.
