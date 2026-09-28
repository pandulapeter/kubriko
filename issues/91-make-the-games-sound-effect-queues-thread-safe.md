# Make the four games' sound-effect queues safe to fill from any thread

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Android, Desktop, iOS (multi-threaded dispatchers; Web is single-threaded)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/AudioManager.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/managers/AudioManager.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/managers/AudioManager.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/managers/AudioManager.kt`

## Problem

Reviewed at `0008d027`. Each game's `AudioManager` batches sound effects per tick in a plain `LinkedHashSet`:

```kotlin
private val soundUrisToPlay = mutableSetOf<String>()
```

It is drained on the tick thread in `onUpdate` — Annoyed Penguins (~line 93), Wallbreaker (~63), Blocky's Journey (~63):

```kotlin
soundUrisToPlay.forEach { soundManager.play(getResourceUri(it, webRootPathName)) }
soundUrisToPlay.clear()
```

and Space Squadron (~63):

```kotlin
if (soundUrisToPlay.isNotEmpty()) {
    val uris = soundUrisToPlay.toList()
    soundUrisToPlay.clear()
    uris.forEach { soundManager.play(getResourceUri(it, webRootPathName)) }
}
```

while `playSoundEffect` adds to it from whatever thread the caller is on. Several callers are off the tick thread:

- Space Squadron: `Bullet.onAdded` (`actors/base/Bullet.kt` ~90) calls `audioManager.playSoundEffect()` for **every shot**, on `ActorManager`'s batch thread (`Dispatchers.Default`).
- Annoyed Penguins: `Star.onRemoved` (`actors/Star.kt` ~66–72) plays the star / level-done sound on the batch thread.
- Wallbreaker: `GameplayManager.onLevelCleared` runs in a `Dispatchers.Default` collector at HEAD (plan 90 moves it to the tick thread, but the queue should not depend on that).
- UI callbacks (hover/click) come from the main thread, which is only the tick thread by convention.

Concurrent `add` during `forEach`/`toList` throws `ConcurrentModificationException` on the tick thread (it propagates out of the frame callback and crashes the app on JVM/Android), can corrupt the `HashSet`, and an `add` landing between iteration and `clear()` silently drops the sound. Space Squadron hits the window on every bullet, so a long session is the likeliest to crash.

## Fix

Apply the same change to all four `AudioManager`s:

```kotlin
private val soundUrisToPlay = MutableStateFlow<Set<String>>(emptySet())
```

```kotlin
override fun onUpdate(deltaTimeInMilliseconds: Int) {
    if (soundUrisToPlay.value.isNotEmpty()) {
        soundUrisToPlay.getAndUpdate { emptySet() }.forEach { soundManager.play(getResourceUri(it, webRootPathName)) }
    }
}
```

In Annoyed Penguins, keep the `timeSinceLastPop` bookkeeping that follows in `onUpdate` unchanged.

```kotlin
private fun playSoundEffect(uri: String) {
    if (userPreferencesManager.areSoundEffectsEnabled.value) {
        soundUrisToPlay.update { it + uri }
    }
}
```

Import `kotlinx.coroutines.flow.getAndUpdate` (and `update`, already imported in all four). `MutableStateFlow.update`/`getAndUpdate` are atomic compare-and-set loops, so an addition either lands before the swap (and is played this tick) or after it (next tick) — never lost, never concurrent with iteration. The per-tick deduplication of the same URI is kept, since the value is still a `Set`.

Allocation: the idle tick reads `.value.isEmpty()` on the shared `emptySet()` singleton and allocates nothing; each `playSoundEffect` allocates one small set, which is per event, not per frame (the old code allocated the `toList()` copy and an iterator per non-empty tick as well).

No `CLAUDE.md` change: none of the four modules documents the queue.

## Tests

None: examples have no test source sets, and the fix replaces the unsynchronised structure with the coroutines library's atomic primitive rather than adding logic of its own.

## Manual check

Desktop (JVM) Showcase, then Android:
1. Space Squadron: collect the multi-shoot power-up and hold Space for two minutes while enemies fire back. No crash; shooting, hit and explosion sounds keep playing throughout.
2. Annoyed Penguins: finish a level — every collected star plays the star sound and the last one plays the level-done sound.
3. Wallbreaker: clear a level — the level-cleared sound plays. Blocky's Journey: menu hover/click sounds still play.
4. Toggle sound effects off in each game's menu: no effects play; toggling back on restores them.
