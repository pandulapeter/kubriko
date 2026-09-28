# Dispose every loaded player in MusicManager.unloadAll() instead of losing them to the cache clear

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Artifact:** `plugin-audio-playback`
**Files:** `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/MusicManagerImpl.kt`, `plugins/audio-playback/src/androidMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.android.kt`, `plugins/audio-playback/CLAUDE.md`

## Problem

```kotlin
override fun unload(uri: String) {
    scope.launch {
        cache.value[uri]?.let { music -> musicPlayer?.dispose(music) }
        cache.update { it.removing(uri) }
    }
}

override fun unloadAll() {
    val curCache = cache.value
    for (c in curCache) {
        unload(c.key)
    }
    cache.update { persistentMapOf() }
}
```

Each `unload()` only *launches* a coroutine on `Dispatchers.Default`, which reads `cache.value[uri]` when it runs.
`unloadAll()` clears the cache synchronously right after launching them, so in practice every coroutine finds
nothing and disposes nothing. The players are no longer reachable from the manager, but keep their resources, and a
track that was playing **keeps playing**: `pause`/`stop`/`isPlaying` look the URI up in the (now empty) cache, and
the focus-loss pause iterates the cache keys, so the music can't be paused or stopped until the process ends (on
desktop and Android the manager's `onDispose` only disposes what is still in the cache).

This is a fix, not a decision: `unload(uri)` already disposes its player, and the KDoc of `unloadAll()` ("Clears the
audio cache") has no other meaning for a playing track than being unloaded.

## Fix

Snapshot, clear, then dispose what was captured:

```kotlin
override fun unloadAll() {
    val unloaded = cache.value
    cache.update { persistentMapOf() }
    val musicPlayer = musicPlayer ?: return
    scope.launch {
        unloaded.values.forEach { music -> if (music != null) musicPlayer.dispose(music) }
    }
}
```

Keep the disposal off the calling thread as `unload` does (desktop's dispose blocks on the audio device). If a
concurrent `unload(uri)` coroutine still reads the old entry, the two paths may dispose the same player twice.
Android's `stop()` before `release()` would then throw `IllegalStateException` on the released `MediaPlayer`, so
make the Android `dispose(cachedMusic)` in `MusicPlayer.android.kt` tolerate a second call (wrap the stop in
`runCatching`; `release()` itself is idempotent) — the other three backends already tolerate it. Plan 33 replaces
this bookkeeping with one that hands each loaded value out exactly once; this plan is the minimal standalone fix.

Delete the `/** unload all data currently in cache */` comment on the override: the member is documented on
`MusicManager`.

## Tests

None on its own: `MusicManagerImpl` only gets a `MusicPlayer` inside `Composable()`, so it can't be driven from a
unit test without a composition. Plan 33 moves this into a testable class and covers `clear()` there.

## Manual check

Desktop or Android, a small test game (or the Audio test temporarily wired to call `musicManager.unloadAll()` from a
button): play a track, call `unloadAll()` → the music stops. Call `play()` for the same URI → it loads again and
plays once (a single copy).

Update `plugins/audio-playback/CLAUDE.md` → MusicManager Internals: "`unloadAll()` clears cache but does not dispose
the manager" → "`unloadAll()` disposes every loaded player (stopping any that play) and clears the cache, without
disposing the manager".
