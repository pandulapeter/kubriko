# Stop the slingshot's stretching sound when the slingshot is removed while it is being aimed

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (audio plays on Android and Desktop)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/slingshot/Slingshot.kt`

## Problem

Reviewed at `0008d027`. The stretching loop is driven by a flag the slingshot raises while a pointer is aiming (`Slingshot.kt` ~line 83):

```kotlin
private var aimingPointerId: PointerId? = null
    set(value) {
        field = value
        audioManager.setShouldPlayStretchingSoundEffect(value != null)
        if (value == null) {
            audioManager.playLaunchSoundEffect()
        }
        activeFakePenguin.isVisible = value != null
    }
```

It is lowered only by a pointer release on that slingshot. `onRemoved` (~line 137) does not touch it:

```kotlin
override fun onRemoved() {
    actorManager.remove(activeFakePenguin, waitingFakePenguin, front)
}
```

`AudioManager` plays `URI_SOUND_STRETCHING` as a looping track whenever `isRunning && areSoundEffectsEnabled && shouldPlayStretchingSound`. When the last star is collected while the player is already pulling back the next penguin, `GameplayManager`'s 600 ms game-end timer sets `currentLevel = null`, `loadScene(null)` calls `actorManager.removeAll()`, and the slingshot is removed with the flag still `true`. The loop pauses only because the game stops running; as soon as the player starts the next level, `isRunning` becomes true and the stretching sound plays continuously until they happen to aim and release once. The same happens when a level is left from the pause menu while a pointer is held on the slingshot.

## Fix

In `Slingshot.onRemoved`, lower the flag if this slingshot was being aimed, without going through the `aimingPointerId` setter (which would also play the launch sound and touch the fake penguin that is being removed):

```kotlin
override fun onRemoved() {
    if (aimingPointerId != null) {
        audioManager.setShouldPlayStretchingSoundEffect(false)
    }
    actorManager.remove(activeFakePenguin, waitingFakePenguin, front)
}
```

The guard matters: `onRemoved` runs on `ActorManager`'s batch thread, after the batch that also added the next level's slingshot has been published, so an unconditional reset could silence a new slingshot the player is already aiming. `setShouldPlayStretchingSoundEffect` is a `MutableStateFlow.update`, safe from any thread. `onRemoved` is only called after `onAdded`, so `audioManager` is initialised — also under the engine lane's planned rule that an actor added and removed in one batch receives both callbacks.

A related behaviour is deliberately left alone: when the window loses focus mid-aim, the pointer-input plugin synthesises a release and the penguin launches. That is a product choice, not a defect of this module.

No `CLAUDE.md` change.

## Tests

None: examples have no test source sets; the behaviour needs a running game, input and audio.

## Manual check

Desktop Showcase (sound effects on), Annoyed Penguins, Map 1:
1. Launch penguins until only one star remains; launch the penguin that will collect it, then immediately press and hold on the slingshot so the stretching sound plays while the star is collected.
2. Keep holding through the fade-out until the level menu appears; release.
3. Start Map 2. Before the fix the stretching sound loops without any aiming; after it, the level starts silent and the sound plays only while aiming.
