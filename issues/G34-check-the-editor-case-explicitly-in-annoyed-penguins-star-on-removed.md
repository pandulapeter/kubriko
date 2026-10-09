# Replace the catch-all in Annoyed Penguins `Star.onRemoved` with an explicit editor check.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/Star.kt`

## Problem
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/Star.kt:66–78`:
```kotlin
override fun onRemoved() {
    try {
        if (!gameplayManager.isLoadingLevel.value) { … play a sound … }
    } catch (_: Exception) {
        // Only happens in the Editor
    }
}
```
The only expected failure is the scene editor case, where `onAdded` left `lateinit var gameplayManager` unset (its own `try { gameplayManager = kubriko.get() } catch (_: IllegalStateException)`). Catching every `Exception` also hides any real error in the sound path.

## Fix
Replace the `try`/`catch` with an early return:
```kotlin
override fun onRemoved() {
    if (!::gameplayManager.isInitialized) return // Only happens in the Editor
    if (!gameplayManager.isLoadingLevel.value) { … unchanged … }
}
```
Leave `onAdded`'s `try`/`catch (_: IllegalStateException)` as it is.

## Behaviour
Same in the game (manager set) and in the editor (manager unset → nothing happens, as before). The only difference: an unexpected exception from `audioManager` or `actorManager` would now surface instead of being swallowed; no such exception is thrown on any current path (`audioManager` is registered in both the game and the editor's `customManagersForSceneEditor`).

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
Collect a star (star sound) and the last star (level-done sound); on desktop, delete a star in the Annoyed Penguins scene editor without a crash.
