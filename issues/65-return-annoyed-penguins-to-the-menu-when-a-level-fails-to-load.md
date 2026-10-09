# Return Annoyed Penguins to the menu when a level fails to load

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** Web (Wasm) mainly; all for malformed scenes
**Challenged:** sound
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/GameplayManager.kt`,
`examples/game-annoyed-penguins/CLAUDE.md`

Ships in no published artifact (`examples/game-annoyed-penguins` is part of the Showcase).

## Problem

`GameplayManager.loadScene` raises `_isLoadingLevel` and only lowers it on success; a failed read is swallowed:

```kotlin
_isLoadingLevel.update { true }
delay(300) // Gives time for the fade animation to hide the previous level
...
try {
    val json = Res.readBytes("files/scenes/$sceneName").decodeToString()
    val newActors = serializationManager.deserializeActors(json)
    actorManager.add(newActors)
    _totalStarCount.update { newActors.filterIsInstance<Star>().count() }
    _isLoadingLevel.update { false }
} catch (_: MissingResourceException) {
}
```

On the web, Compose resources 1.12.1 (`DefaultWasmResourceReader.readAsBlob`) throws `MissingResourceException` both for
an HTTP error (`!response.ok`, e.g. a 404 after a redeploy) and for a failed `fetch` (offline, dropped connection) — it
wraps the fetch in `catch (_: Throwable) { throw MissingResourceException(resPath) }`. Either way the catch above leaves
`isLoadingLevel == true` with the game running: `AnnoyedPenguinsGame` keeps the viewport at alpha 0, hides the HUD
(`isGameRunning && !isLoadingLevel`) and shows the loading indicator (`!isGameLoaded || isLoadingLevel`) forever.
Pausing brings the menu back, but picking the same level again is a no-op (`setCurrentLevel` ignores the level that is
already current), so that level can never be retried.

Two further cases fall through:

- An error after the response arrives — `response.blob().await()` rejecting mid-body, or a Cache API failure in
  `ResourceWebCache` — surfaces as a non-`MissingResourceException` exception and escapes the `scope.launch` in
  `onInitialize`. The Kubriko scope is a `SupervisorJob` with no handler, so it goes to the platform's uncaught-exception
  handler (a console error on the web, a crash on Android) and the level collector dies: no level can be loaded again.
- `SerializationManager.deserializeActors` catches its own errors and returns an empty list, so a malformed scene loads
  as an empty level with `totalStarCount == 0`, which never ends.

A subtlety for the fix: the web reader turns a *cancelled* fetch into `MissingResourceException` too (its
`catch (_: Throwable)` also catches the `CancellationException` from `suspendCancellableCoroutine`). `collectLatest`
cancels a load when the player picks another level, so a catch that resets `currentLevel` must first check that the
coroutine is still active, or it would clobber the newly chosen level.

## Fix

In `loadScene`'s `else` branch:

```kotlin
try {
    val json = Res.readBytes("files/scenes/$sceneName").decodeToString()
    val newActors = serializationManager.deserializeActors(json)
    if (newActors.isEmpty()) {
        onLevelLoadFailed()
    } else {
        actorManager.add(newActors)
        _totalStarCount.update { newActors.filterIsInstance<Star>().count() }
        _isLoadingLevel.update { false }
    }
} catch (exception: CancellationException) {
    throw exception
} catch (_: Exception) {
    currentCoroutineContext().ensureActive()
    onLevelLoadFailed()
}
```

with

```kotlin
private fun onLevelLoadFailed() {
    _isLoadingLevel.update { false }
    _currentLevel.update { null }
}
```

Setting `currentLevel` to null makes the `collectLatest` collector run `loadScene(null)`, which clears the scene, pauses
(`stateManager.updateIsRunning(false)`) and restores the viewport alpha — the player is back on the menu and can pick the
same level again. `currentCoroutineContext().ensureActive()` rethrows when the failure was really a cancellation by a
newer level choice. Drop the now-unused `MissingResourceException` import; add `kotlinx.coroutines.CancellationException`,
`kotlinx.coroutines.currentCoroutineContext` and `kotlinx.coroutines.ensureActive`. No error message is shown (the game
has no error UI and the menu reappearing is the feedback); adding one would need a new string in the module's
`strings.xml` — not proposed.

The other examples that read a scene the same way (`demo-physics`, `demo-performance`, `game-blockys-journey`) do not hang
on failure — they show an empty scene — so they are left alone.

In `examples/game-annoyed-penguins/CLAUDE.md`, after "A level change cancels a load still in progress (`collectLatest`).",
add: "A level that fails to load (missing or unreachable file, or a scene that deserializes to nothing) resets
`currentLevel` to null, which returns to the menu."

## Tests

None. `GameplayManager` reads a fixed scene file per `Level` enum entry through the generated `Res`, so a failure cannot
be injected without restructuring the manager for testability, and it needs `PhysicsManager`, `SerializationManager` and
the game's `ViewportManager` setup. The failure path is three state updates; the manual check covers it.

## Manual check

Run the Showcase in a browser (`./gradlew :app:web:wasmJsBrowserDevelopmentRun`), open Annoyed Penguins, then in DevTools
→ Network either block the request URL pattern `*files/scenes/*` or switch to Offline, and pick a level: the loading
indicator should disappear and the menu return. Unblock and pick the same level: it loads. Also pick level A, and
before it finishes loading (throttle to Slow 3G) pick level B from the paused menu: level B loads (the cancellation is
not mistaken for a failure).
