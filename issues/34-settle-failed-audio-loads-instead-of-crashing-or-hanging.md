# Catch audio load failures on every platform and settle them, instead of crashing the app or leaving loading progress stuck

**Challenged:** amended — a retry must not un-settle a failed URI: every Showcase game shows its game viewport only while `getLoadingProgress == 1f` (`isGameLoaded()` → `AnimatedVisibility`), so a retry triggered by `play()` of a broken sound effect would hide the game and show the loading spinner on every hover or brick pop; the failed entry now stays counted as settled while its retry runs, and the marker lives in plan 33's single slot map.

**Decision needed:** Should a URI whose load failed count as settled in `getLoadingProgress` (so progress still
reaches 1 and a loading screen can finish), with `play()`/`preload()` retrying it? — recommended: **yes, count it
as settled and retry on the next `preload()`/`play()`**.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all (crash on Android, hang on desktop/web/iOS)
**Artifact:** `plugin-audio-playback` — changes the observable values of `MusicManager.getLoadingProgress` and
`SoundManager.getLoadingProgress` for failed URIs (under the recommended option)
**Depends on:** plan 00, plan 33 (`AudioCache`), plan 32 (Android sounds already report failure as `null`)
**Files:** `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/AudioCache.kt`, `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/MusicManager.kt`, `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/SoundManager.kt`, `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/MusicManagerImpl.kt`, `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/SoundManagerImpl.kt`, `plugins/audio-playback/src/androidMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.android.kt`, `plugins/audio-playback/src/desktopMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.desktop.kt`, `plugins/audio-playback/src/desktopMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/DesktopMusicPlayer.kt`, `plugins/audio-playback/src/webMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.web.kt`, `plugins/audio-playback/src/webMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/WebMusicPlayer.kt`, `plugins/audio-playback/src/iosMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.ios.kt`, `plugins/audio-playback/src/iosMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/SoundPlayer.ios.kt`, `plugins/audio-playback/src/desktopTest/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/AudioCacheTest.kt`, `plugins/audio-playback/CLAUDE.md`, `plugins/audio-playback/README.md`

## Problem

A missing, mistyped or undecodable file (or a failed fetch on the web) is handled differently on every platform,
and badly on all of them. The Kubriko scope is `SupervisorJob() + Dispatchers.Default` without a
`CoroutineExceptionHandler`, so an exception that escapes a manager's `scope.launch` goes to the thread's uncaught
exception handler, which **terminates the app on Android and iOS**.

- **Android music** (`MusicPlayer.android.kt`): `context.getFileDescriptor(uri)` (`assets.openFd`) throws
  `FileNotFoundException`, and `MediaPlayer.prepare()` throws `IOException` for an unsupported file, inside
  `suspendCoroutine { … }` → the exception escapes `MusicManagerImpl`'s `scope.launch` → **crash**. The half-built
  `MediaPlayer` is never released.
- **Desktop music** (`MusicPlayer.desktop.kt`): `URL.openStream()` / `FileInputStream` throw `IOException`,
  `DesktopMusicPlayer`'s constructor throws `IllegalArgumentException` for a null stream → printed as an uncaught
  exception, the entry stays `null` ("loading") forever.
- **Web music** (`WebMusicPlayer.kt`): the load runs in `init { scope.launch { window.fetch(uri).await(); …
  decodeAudioData(…).await(); onPreloadReady(this) } }`. A network error or a rejected `decodeAudioData` (a 404
  page is not audio) throws inside that launch, and `onPreloadReady` is never called — so the `suspendCoroutine` in
  `MusicPlayer.web.kt`'s `preload` **never resumes**, and the entry stays "loading" forever.
- **iOS music and sounds**: `NSURL.URLWithString(URLString = uri)!!` throws on a malformed URI, and
  `AVAudioPlayer(url, error = null)` ignores the error for a missing file.
- **Both managers**: a platform `preload` that returns `null` (desktop sounds already do, Android sounds do after
  plan 32) leaves the entry `null`, which `getLoadingProgress` counts as not loaded:
  ```kotlin
  cache.filter { (key, _) -> key in uris }.count { (_, value) -> value != null }.toFloat() / uris.size
  ```
  Every example game gates its loading screen on `getLoadingProgress(...) == 1f`, so one bad file keeps the game on
  its loading screen forever, with nothing logged at a visible level.

## Fix

### Platform side: every `preload` returns `null` on failure and never throws (except `CancellationException`)

- **Android music**: open the descriptor and prepare inside `try`; on `IOException`/`IllegalStateException` release
  the `MediaPlayer` and return `null`. `prepare()` is synchronous, so drop the `suspendCoroutine` +
  `setOnPreparedListener` wrapper and return the player after `prepare()` returns (the listener only ever fired after
  it anyway). Close the `AssetFileDescriptor` with `use`.
- **Desktop music**: wrap stream opening and `DesktopMusicPlayer(...)` in `try`; catch `IOException` and
  `IllegalArgumentException` (and JLayer's `JavaLayerException` if plan 30 left anything in the constructor that
  throws it), close the stream, return `null`. Make the constructor take a non-null `InputStream` and drop its
  `IllegalArgumentException`.
- **Web music**: replace the `init` launch and the `onPreloadReady` callback with a suspend factory, e.g.
  `suspend fun WebMusicPlayer.Companion.load(scope, uri): WebMusicPlayer?`, which awaits `fetch`, returns `null`
  when `!response.ok`, then awaits `arrayBuffer()` and `decodeAudioData`, all inside
  `try { … } catch (e: Throwable) { if (e is CancellationException) throw e; null }` (a rejected promise surfaces
  from `await()` as a `Throwable`). `MusicPlayer.web.kt`'s `preload` calls it directly; the `suspendCoroutine`
  goes away. (`ok` needs to be read from the `Response`; it is in `org.w3c.fetch.Response`.)
- **iOS music and sounds**: `NSURL.URLWithString(uri) ?: return null`; create the player with an `NSError` out
  parameter (`memScoped { val error = alloc<ObjCObjectVar<NSError?>>(); AVAudioPlayer(url, error.ptr) }`) and
  return `null` when `error.value != null`; wrap in `runCatching` as a backstop.

### Manager side (`AudioCache` from plan 33)

**Option A (recommended): a failed URI is settled.** `AudioCache` stores a private `LoadFailed` marker as the entry's
value when the loader returns `null` or throws. Consequences:
- `getLoadingProgress` counts the marker as settled with no code change (it is non-null), so progress reaches 1.
- Every read of a loaded value goes through an accessor that maps the marker to `null` (`isPlaying`, `pause`, `stop`,
  `setVolume`, `remove`, `clear` must never hand the marker to a player — it would be cast to `MediaPlayer` etc.).
- `preload(uri)` and `get(uri)` on a failed URI start a new load (a transient network error on the web recovers on
  the next `play()`), but the entry **stays counted as settled while that retry runs**: keep it as a failed slot that
  carries the retry's `Deferred` (e.g. `Failed(retry: Deferred<Any?>?)` in plan 33's slot map), not as a `Loading`
  slot, so `entries` keeps reporting it non-null until the retry succeeds (→ `Loaded`) or fails again (→ `Failed`
  with no retry). Progress for a URI never drops back below a value it already reached. This matters: every
  Showcase game renders its game viewport only while `isGameLoaded()` (progress `== 1f`) is true, and the games call
  `soundManager.play()` on every hover, bounce and hit, so a dip would swap the running game for the loading spinner
  (and, with engine plan 13, report the viewport unfocused and pause the music) each time a broken sound is played.
  At most one retry per URI is in flight; further `get()` calls join it.
- The manager logs the failure at `Logger.Importance.HIGH` with the URI.

**Option B: a failed URI stays unsettled.** Nothing crashes or waits forever, but `getLoadingProgress` never reaches 1
for it, as today on desktop. No marker is stored (the entry stays `null`), `get()` returns `null`, and retrying
works the same way. KDoc unchanged. Choose this only if a game should be able to tell "failed" from "done" through
the progress value.

Under option A, add one sentence to the KDoc of both `getLoadingProgress` overloads in `MusicManager` and the one in
`SoundManager`: a file that fails to load counts as settled, so the progress still reaches 1, and playing it again
retries the load.

## Tests

Extend `AudioCacheTest` (desktopTest):
1. A loader that returns `null` → the entry is settled (option A: counted by the progress formula; option B: still
   `null`), and the value accessor returns `null`.
2. A loader that throws `IOException` → same as 1, and the exception does not escape (the test's scope is not
   cancelled; use a `SupervisorJob` scope and assert it is still active).
3. After a failure, `get(uri)` calls the loader again, and a successful second attempt stores the value.
   While that second attempt is suspended, the entry is still counted by the progress formula (option A), and a
   concurrent second `get(uri)` does not call the loader a third time.
4. A loader cancelled with `CancellationException` does not store the marker.

The platform catches can't be unit-tested (they need `AssetManager`, `AVAudioPlayer`, the browser's `fetch`, and a
JavaSound device for a real decode).

## Manual check

On each platform, in a copy of Wallbreaker with one music and one sound URI misspelled:
- Android and iOS: no crash; the game leaves its loading screen (option A); the valid sounds play; the debug menu
  log shows a HIGH entry naming the bad URI.
- Desktop: no stack trace in the console; same as above.
- Web: in DevTools set Network → Offline after the page loads but before the music loads (or block the music URL):
  the loading screen finishes (option A); going back online and re-entering the game plays the music.

Update `plugins/audio-playback/CLAUDE.md` (MusicManager Internals: how failures are recorded, and that `null` from a
platform `preload` means failure) and add a line on failed files under the README's usage section.
