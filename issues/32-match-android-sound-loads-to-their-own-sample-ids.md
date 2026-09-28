# Resume each Android sound load with its own sample id, and never leave one waiting forever

**Challenged:** sound

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** Android
**Artifact:** `plugin-audio-playback` (internal change only)
**Files:** `plugins/audio-playback/src/androidMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/SoundPlayer.android.kt`

## Problem

```kotlin
private var preloadListeners = mutableListOf<PreloadListener>()
...
setOnLoadCompleteListener { _, sampleId, status ->
    if (status == 0) {
        preloadListeners.forEach { it.onSampleLoaded(sampleId) }
    }
}
...
override suspend fun preload(uri: String) = withContext(Dispatchers.IO) {
    suspendCoroutine { continuation ->
        val preloadListener = object : PreloadListener {
            override fun onSampleLoaded(sampleId: Int) {
                preloadListeners.remove(this)
                continuation.resume(sampleId)
            }
        }
        preloadListeners.add(preloadListener)
        soundPool.load(context.getFileDescriptor(uri), 1)
    }
}
```

With more than one load pending (`SoundManager.preload(a, b, c)` after the first composition launches one coroutine
per URI; a `play()` of a sound that is still loading starts a second load):

- **Wrong sounds.** Every listener is offered every completed `sampleId`, and the first one takes it. Loads finish in
  whatever order `SoundPool` decodes them, so sound A's `preload` can resume with B's id: from then on playing A
  plays B.
- **Crash.** `remove(this)` inside `forEach` modifies the `ArrayList` being iterated: with three or more pending
  loads the next `next()` throws `ConcurrentModificationException` — on the main thread, where `SoundPool` delivers
  load events (the listener is registered from `remember` during composition), so the app crashes. With exactly two
  pending, the second listener is silently skipped instead. The list is also written from `Dispatchers.IO` threads
  while the main thread iterates it.
- **Hang.** A load that fails (`status != 0`) resumes nobody. The continuation waits forever, and
  `SoundManagerImpl.Composable` preloads the queued URIs **one after another** in a single coroutine, so every later
  sound never loads and `getLoadingProgress` never reaches 1 (a game gating on it stays on its loading screen).
- **Crash.** `assets.openFd()` throws `FileNotFoundException` for a missing asset (and `IOException` for a
  compressed one); it is thrown inside the `suspendCoroutine` block, escapes into the Kubriko scope
  (`SupervisorJob() + Dispatchers.Default`, no handler) and kills the app. The listener it already registered stays
  in the list and later steals another load's id.
- **Leak.** The `AssetFileDescriptor` passed to `soundPool.load` is never closed (`SoundPool` dups the descriptor
  and does not close the caller's).

## Fix

Key pending loads by the id `soundPool.load()` returns, and touch that map on the main thread only, which is where
`SoundPool` delivers its callbacks:

```kotlin
private val pendingLoads = HashMap<Int, CancellableContinuation<Int?>>()   // main thread only

setOnLoadCompleteListener { pool, sampleId, status ->
    val continuation = pendingLoads.remove(sampleId) ?: return@setOnLoadCompleteListener
    if (status == 0) {
        continuation.resume(sampleId)
    } else {
        pool.unload(sampleId)
        continuation.resume(null)
    }
}

override suspend fun preload(uri: String): Int? {
    val fileDescriptor = withContext(Dispatchers.IO) {
        try { context.getFileDescriptor(uri) } catch (_: IOException) { null }
    } ?: return null
    return withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val sampleId = fileDescriptor.use { soundPool.load(it, 1) }
            if (sampleId == 0) { continuation.resume(null); return@suspendCancellableCoroutine }
            pendingLoads[sampleId] = continuation
            continuation.invokeOnCancellation { /* on Main: */ pendingLoads.remove(sampleId); soundPool.unload(sampleId) }
        }
    }
}
```

Registration and the callback both run on the main looper, and the callback is posted to it, so it can't arrive
before the id is registered. `invokeOnCancellation` may run on another thread: post its body to the main thread
(e.g. `Handler(Looper.getMainLooper()).post { … }`) or do the removal in the listener by checking
`continuation.isActive` — pick one and keep all `pendingLoads` access on main. `soundPool.load` returns 0 on
failure. `AssetFileDescriptor` is `Closeable`, so `use` closes it once `load` has dup'ed it. Delete the
`PreloadListener` interface and the list. A `null` result is how a failed load is reported to `SoundManagerImpl`
(plan 34 decides what the manager does with it; until then the entry stays "loading", as it does on desktop today).

## Tests

None: the logic that remains is a map keyed by the id `SoundPool` hands out, on one thread, and `SoundPool` itself
(plus `AssetManager`) only exists on a device — the plugin has no Android host tests.

## Manual check

Android device, a build of any Showcase game that preloads several sounds (Wallbreaker, Space Squadron):
1. Temporarily call `soundManager.preload(...)` for all its sounds from a button *after* the game is on screen (so
   the loads run in parallel): no crash, and each sound effect in the game plays its own sound.
2. Add a non-existent URI to the preload list: no crash; the other sounds still load and play.
3. `adb shell` StrictMode or `CloseGuard` logging (`StrictMode.VmPolicy.Builder().detectLeakedClosableObjects()`)
   reports no leaked `ParcelFileDescriptor` after preloading.
