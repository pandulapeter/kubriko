# Make deserializeActors fail the same way for every kind of bad actor instead of crashing on some

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `plugin-serialization`
**Files:** `plugins/serialization/src/commonMain/kotlin/com/pandulapeter/kubriko/serialization/SerializationManagerImpl.kt`, `plugins/serialization/src/commonMain/kotlin/com/pandulapeter/kubriko/serialization/SerializationManager.kt` (KDoc), `plugins/serialization/src/commonTest/kotlin/com/pandulapeter/kubriko/serialization/DeserializeActorsTest.kt` (new), `plugins/serialization/CLAUDE.md`, `plugins/serialization/README.md`

**Decision needed:** when one actor in a scene cannot be restored, should `deserializeActors` return an empty list for the whole scene, or skip that actor and return the rest? — recommended: **A, all-or-nothing: an empty list for any failure, logged** (today's behaviour for `SerializationException`, extended to every exception). The tools lane (scene editor) is aligned to this: it treats an empty result for a non-empty input as a failed load.

## Problem

`SerializationManagerImpl.deserializeActors` (at 0008d027):

```kotlin
override fun deserializeActors(
    serializedStates: String,
) = try {
    json.decodeFromString<List<ActorStateWrapper>>(serializedStates).mapNotNull { wrapper ->
        typeIdsToDeserializers[wrapper.typeId]?.invoke(wrapper.serializedState)?.restore()
    }
} catch (_: SerializationException) {
    emptyList()
}
```

- A `SerializationException` anywhere — the outer JSON, or **one** actor's `deserializeState` — silently yields an empty scene, with nothing logged, so a single corrupt actor makes a level load as empty with no hint why.
- Any other exception from a consumer's `deserializeState` or `restore()` (an `IllegalArgumentException` from a `require` in an actor's constructor, a `NumberFormatException` in a hand-written parser, a `NullPointerException`) **propagates**. Callers such as `game-blockys-journey`'s `LoadingManager` call it inside a coroutine on the Kubriko scope, where an uncaught exception reaches the platform handler — a crash on Android/iOS.
- The KDoc says only "Deserializes a string representation into a list of actors", and `plugins/serialization/CLAUDE.md` says the empty list is returned for "a `SerializationException` from the outer JSON parse", which is narrower than what the code does.

So the same kind of bad input either empties the scene or crashes the game, depending on which exception the consumer's code happens to throw.

## Fix

- **A (recommended) — all-or-nothing.** Catch `Throwable` around the whole decode-and-restore, rethrowing `CancellationException` (the function is not `suspend`, but a consumer's `restore()` might run blocking code that throws it; keep the rethrow cheap and explicit), log it with `log(message = "Failed to deserialize actors", details = throwable.message)`, and return `emptyList()`. Unknown `typeId`s keep being skipped silently, as documented. Rationale: a partially restored level is usually broken in ways that are harder to notice than an empty one (the player actor missing, a door without its key), and an editor that loads a partial scene and saves it destroys the actors it could not read. KDoc on `deserializeActors`: "Returns an empty list when [serializedStates] is not a valid scene or any registered actor in it cannot be restored; actors whose type ID is not registered are skipped." Note in the KDoc that an empty scene also returns an empty list, so a caller that must tell "empty" from "failed" checks the input (an empty scene serializes as `[]`).
- **B — per-actor skip.** Catch per wrapper inside `mapNotNull`, log and skip the failing actor, return the rest; the outer JSON failing still returns `emptyList()`. Better for games that prefer "most of the level" over nothing; worse for the editor (silent partial load, then data loss on save), which would then need a separate way to learn that something was skipped.

Either way update `plugins/serialization/CLAUDE.md` → "Type registration" / "Save/load flow" and add one sentence to `README.md` → "4. Serialize and Deserialize".

## Tests

`DeserializeActorsTest` in `commonTest` with two tiny `Serializable` actor fixtures registered via `SerializableMetadata`: one whose state round-trips a `Float` with `Json`, one whose `restore()` throws `IllegalArgumentException` when the stored value is negative.
- A scene of three valid actors round-trips (`serializeActors` → `deserializeActors`) with the same count and values.
- A scene where one actor's inner state string is not valid JSON → `emptyList()` (A) / the two others (B), and no exception.
- A scene where one actor's `restore()` throws `IllegalArgumentException` → `emptyList()` (A) / the others (B), and no exception (today it throws).
- An unknown `typeId` in an otherwise valid scene → skipped, the rest returned.
- `"not json"` → `emptyList()`; `"[]"` → `emptyList()`.

Run `./gradlew :plugins:serialization:desktopTest`.

## Manual check

Showcase → Blocky's Journey still loads its scene (`./gradlew :app:desktop:run`).
