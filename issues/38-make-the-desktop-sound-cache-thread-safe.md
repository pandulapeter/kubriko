# Make the desktop sound player's cache map thread-safe

**Challenged:** amended — the map is deleted instead of made concurrent: after plan 33 it is redundant, and its "return the cached instance" shortcut hands the same `CachedSound` to two loads (e.g. `unload(uri)` then `play(uri)` before the `GlobalScope` dispose has run), so `AudioCache` stores an instance that is then disposed under it — which breaks plan 33's "each value handed out once" invariant.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** desktop
**Artifact:** `plugin-audio-playback` (internal change only)
**Files:** `plugins/audio-playback/src/desktopMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/SoundPlayer.desktop.kt`

## Problem

```kotlin
private val cachedSounds = mutableMapOf<String, CachedSound>()

override suspend fun preload(uri: String): Any? = withContext(Dispatchers.IO) {
    cachedSounds[uri]?.let { return@withContext it }
    ...
    cachedSounds[uri] = cachedSound
    ...
}

override fun dispose(cachedSound: Any) {
    GlobalScope.launch(Dispatchers.IO) {
        val sound = cachedSound as CachedSound
        cachedSounds.remove(sound.uri)
        sound.dispose()
    }
}
```

`SoundManager.preload(a, b, c)` after the first composition launches one coroutine per URI, and each runs this body
on its own `Dispatchers.IO` thread; `dispose` removes from yet another thread. `mutableMapOf` is a `LinkedHashMap`:
concurrent `put`s can lose entries or corrupt the table during a resize (on JDK 8+ this shows up as lost updates or,
with tree bins, as a `ClassCastException`/infinite loop in rare cases). A lost entry only costs a re-decode, but a
corrupted table can throw from `preload`, which on desktop is printed as an uncaught exception and leaves the sound
unloaded.

`dispose(sound)` also removes by key only, so a dispose that runs late (it is launched on `GlobalScope`) can remove a
**newer** `CachedSound` preloaded for the same URI after an `unload`.

## Fix

After plan 33, the managers' `AudioCache` guarantees one load per URI and disposes every value exactly once, so the
player's own `cachedSounds` map only does harm: `preload` returns the instance it holds for the URI, and `dispose`
removes it later on `GlobalScope`. `unload(uri)` followed by a `play(uri)`/`preload(uri)` before that coroutine runs
makes the new load return the **same** `CachedSound` that is about to be disposed; `AudioCache` stores it, and the
next `play` finds its clips closed. Two loads racing across a `remove` (plan 33's test 6) can do the same.

- Delete `cachedSounds`, the early `cachedSounds[uri]?.let { return@withContext it }` and the
  `cachedSounds[uri] = cachedSound` in `preload`: every load decodes its own `CachedSound`.
- `dispose(cachedSound)` only disposes the sound it was given (keep it off the caller thread as today);
  `dispose()` has nothing left to clear and becomes `Unit`.

If plan 33 was not landed in this lane, fall back to the original fix instead: `ConcurrentHashMap` and
`cachedSounds.remove(sound.uri, sound)` in `dispose`.

## Tests

None: `SoundPlayer.desktop.kt` is created inside a composition and needs a JavaSound mixer; the change removes state
rather than adding logic, and plan 33's `AudioCacheTest` covers the hand-out-once invariant it restores.

## Manual check

Desktop Showcase: open Wallbreaker, Space Squadron and Blocky's Journey one after another (each preloads a set of
sounds in parallel); every sound effect plays, and the console shows no exception from `SoundPlayer.desktop.kt`.
