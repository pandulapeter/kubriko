# Share one in-flight load per URI between preload() and play(), so no player is created twice or orphaned

**Challenged:** amended — the loaded values and the in-flight loads must live in one atomically updated map (two separate flows let a `clear()`/`remove()` on the game thread slip between a finishing load's identity check and its store, resurrecting the entry and breaking "disposed exactly once"), with a concurrency test; `entries` becomes a derived read-only view.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all
**Artifact:** `plugin-audio-playback` (internal change only; `getLoadingProgress` keeps its current values)
**Depends on:** plan 00 (kotlin-test in `desktopTest`), plan 32 (Android sound loads may run in parallel only after it)
**Files:** `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/AudioCache.kt` (new), `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/MusicManagerImpl.kt`, `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/SoundManagerImpl.kt`, `plugins/audio-playback/src/*/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicManagerDisposer*.kt` (common expect + four actuals, only if the signature changes), `plugins/audio-playback/src/desktopTest/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/AudioCacheTest.kt` (new), `plugins/audio-playback/CLAUDE.md`

## Problem

Both managers keep `cache: MutableStateFlow<PersistentMap<String, Any?>>`, where `null` means "loading", but the
load itself is not recorded anywhere, so nothing stops a second one. `MusicManagerImpl.play`:

```kotlin
val cachedSound = cache.value[uri]
scope.launch {
    if (cachedSound == null) {
        musicPlayer.preload(uri)?.let { music ->
            addToCache(uri, music)
            if (stateManager.isFocused.value) { ... musicPlayer.play(music, shouldLoop, shouldRestart) }
        }
    } else { ... }
}
```

- `play()` while a `preload()` of the same URI is still running (the cache entry is `null`) loads a **second
  player**. Both `addToCache` calls land, the later one wins, and the other player is orphaned. When the orphan is
  the one `play()` started — the usual case, since the example games call `play()` from a focus collector while
  `LoadingManager` preloads — the music plays from a player the manager no longer references: `pause()` on focus
  loss, `stop()` and `isPlaying()` all act on the other one, so the track keeps playing in the background and can't
  be stopped.
- `play()` of a URI that was never preloaded doesn't even add a `null` entry, so every call until the first load
  finishes starts another load: a game that calls `play()` again before the first completes (a focus flicker, a
  retry in `onUpdate`) gets several copies of the track playing on top of each other.
- `unload(uri)` during a load finds `null`, removes the entry, and then the load completes and `addToCache` puts it
  back: the "unloaded" track resurrects.
- `SoundManagerImpl.play` has the same double load for a sound that is still loading (on Android the first sample
  is never unloaded).
- `onDispose` disposes only what is in the cache: a load that completes after it leaks its player.

## Fix

Move the bookkeeping of both managers into one internal class, `AudioCache`, in
`audioPlayback/implementation/AudioCache.kt`. Its contract:

- `val entries: Flow<Map<String, Any?>>` — the same shape as today (`null` = loading, non-null = loaded), derived
  from the single slot map (rule 0), so `getLoadingProgress` keeps its exact values and formula; and
  `fun loaded(uri: String): Any?` for synchronous reads.
- `fun preload(uri: String)` — adds a `null` entry if the URI is unknown, and starts its load if a loader is
  attached. No-op for a URI that is loading or loaded.
- `suspend fun get(uri: String): Any?` — the loaded value; otherwise joins the load in flight; otherwise adds the
  entry and starts one. `null` when no loader is attached yet or the load returned `null`.
- `fun attach(scope: CoroutineScope, loader: suspend (String) -> Any?, onDiscarded: (Any) -> Unit)` — called once
  from the manager's `Composable()` when the player is created; starts a load for every entry still `null` (each its
  own coroutine; plan 32 makes that safe on Android).
- `fun remove(uri: String): Any?` — drops the entry synchronously and returns its loaded value for the caller to
  dispose; a load in flight for it is abandoned, and its result goes to `onDiscarded` when it arrives.
- `fun clear(): List<Any>` — the same for every entry at once (used by `unloadAll` and `onDispose`).
- No constructor parameters, so it can be a plain property of the manager, exactly like `cache` is today, and
  `preload()` keeps working before the manager is initialised or composed (another Manager's `onInitialize` calling
  `preload`, as every example's `LoadingManager` does).

Implementation rules (the executor chooses the code, these are the invariants the tests check):

0. **One source of truth.** Keep everything per URI in a single
   `MutableStateFlow<PersistentMap<String, Slot>>`, where the private `Slot` is either `Loading(deferred)` or
   `Loaded(value)` (plus plan 34's failure marker later). Every mutation is a `compareAndSet` loop on that one flow,
   and every decision — who starts a load, whether a finished result is stored or discarded, what `remove`/`clear`
   return — is taken from the snapshot whose `compareAndSet` won. Do **not** keep the in-flight loads and the loaded
   values in two flows: the manager's `unload`/`unloadAll` run on the game thread while loads finish on
   `Dispatchers.Default`, and a `clear()` landing between a finishing load's identity check and its write would
   return nothing for that URI and then see the value written back (a resurrected, never-disposed player).
   `entries` is then a read-only view, not a second store: `val entries: Flow<Map<String, Any?>>` (the slot map
   mapped to `null` for loading and the value for loaded — `getLoadingProgress` keeps its formula and values) plus
   `fun loaded(uri: String): Any?` for the synchronous reads (`isPlaying`, `pause`, `stop`, `setVolume`).
1. **One load per URI.** Start one with `scope.async(start = CoroutineStart.LAZY) { loader(uri) }`, publish its
   `Loading` slot with `compareAndSet`, and only `start()` it if the publish won; a lost race `cancel()`s the
   unstarted one and uses the winner. No `synchronized` is needed (common code) and nothing blocks.
2. **A result is stored only by the load that is still current.** On completion, replace the slot with `Loaded`
   only if it is still `Loading` with *this* `Deferred` (identity), in the same `compareAndSet`; otherwise
   (removed, cleared, replaced) hand a non-null result to `onDiscarded`.
3. Every value is handed out for disposal exactly once: by `remove`/`clear`, or to `onDiscarded`.
4. `get()` callers that were waiting on an abandoned load receive `null`.

Then, in `MusicManagerImpl` and `SoundManagerImpl`:

- Replace `cache` and `addToCache` with `private val audioCache = AudioCache()`.
- `Composable()`: create the player, then `audioCache.attach(scope, player::preload) { player.dispose(it) }` in
  place of the sequential `cache.value.keys.forEach { … }` loop. The `onDiscarded` lambda captures the local player,
  not the nullable field, so a load that completes after `onDispose` is still released.
- `play()`: `scope.launch { val music = audioCache.get(uri) ?: return@launch; … }` — the `isFocused` check, volume
  application and `play` call stay as they are. `play()` before the first composition stays a no-op, as today.
- `isPlaying`, `pause`, `stop`, `setVolume` read `audioCache.loaded(uri)` where they read `cache.value[uri]`.
- `unload(uri)`: `audioCache.remove(uri)?.let { music -> scope.launch { player?.dispose(music) } }`.
- `unloadAll()`: `audioCache.clear()` and dispose the returned values off the caller thread (replacing plan 31's
  body).
- `onDispose()`: dispose `audioCache.clear()` through the existing `onManagerDisposed` path (change
  `MusicPlayer.onManagerDisposed(cache: PersistentMap<String, Any?>)` to take the list, in all four
  `MusicManagerDisposer.*.kt` files, if that is simpler than rebuilding a map) — after that, a late load's result
  goes to `onDiscarded`. Note the Kubriko scope is cancelled on dispose, so a load in flight is usually cancelled
  rather than completed; a platform `preload` that finishes creating its player anyway (a non-cancellable
  `withContext(Dispatchers.IO)` block) returns it to a cancelled caller, which is the one leak this plan can't close.
- Keep the "Preloading…" / "… preloaded." log messages: log from the manager around `preload`/`attach`, or pass a
  logging callback into `AudioCache`.

## Tests

`AudioCacheTest` in `desktopTest` (`runTest` from the `kotlinx-coroutines-test` plan 00 adds, or `runBlocking`; the
class is common code, so `commonTest` works too if `runTest` is used), with a fake loader that counts calls and
completes from a `CompletableDeferred` per URI. Run with `./gradlew :plugins:audio-playback:desktopTest`.

1. `get(uri)` called twice concurrently before the load completes → the loader runs once, both get the same value.
2. `preload(uri)` then `get(uri)` → one loader call.
3. `preload(uri)` before `attach` → `entries.first()[uri]` is `null` (the key present), the loader is not called; after `attach` it is
   called once and the entry becomes the value.
4. `remove(uri)` while loading → `entries` no longer contains `uri` immediately; completing the load passes the
   value to `onDiscarded` and does not re-add it; a waiting `get` returns `null`.
5. `clear()` with two loaded and one loading entry → returns the two values, `entries` is empty; completing the third
   load passes it to `onDiscarded`.
6. After `remove(uri)` and a fresh `get(uri)`, the late result of the abandoned load does not overwrite the new
   value (identity check).
7. Concurrency (desktopTest, real threads): repeat ~1000 times — start a load, then complete it on one thread while
   another calls `clear()`; afterwards the value was handed out exactly once in total (returned by `clear()` or
   passed to `onDiscarded`, never both, never neither) and `entries` is empty afterwards. The invariant always holds under rule 0; a two-flow implementation
   breaks it intermittently.

## Manual check

Android and desktop, Wallbreaker or Space Squadron from a cold start (so the music is still loading when the focus
collector calls `play()`): exactly one copy of the music plays; switching to another app/window silences it, and
coming back resumes a single copy. The Audio test: play, stop and play the tracks repeatedly — never two copies.

Update `plugins/audio-playback/CLAUDE.md`: add `implementation/AudioCache.kt` to Key Files (one in-flight load per
URI, shared by preload and play); in MusicManager Internals replace "Cache is `MutableStateFlow<PersistentMap<String,
Any?>>` — `null` = loading in progress" with the `AudioCache` description, and correct "`play()` before composition
queues a deferred load" in "Critical: Players Are Created in `Composable()`" — `play()` before the first composition
is ignored; `preload()` is what queues a load.
