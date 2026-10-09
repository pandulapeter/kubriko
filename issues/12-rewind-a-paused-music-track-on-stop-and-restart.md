# Rewind a paused music track on stop and restart

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all (stop: all; restart: Android, iOS, Web)
**Challenged:** sound
**Files:** `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/MusicManagerImpl.kt`, `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/MusicManager.kt` (KDoc of `play`), `plugins/audio-playback/src/androidMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.android.kt`, `plugins/audio-playback/src/iosMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.ios.kt`, `plugins/audio-playback/src/webMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/WebMusicPlayer.kt`, `plugins/audio-playback/src/desktopTest/kotlin/com/pandulapeter/kubriko/audioPlayback/MusicManagerTest.kt` (or a new `MusicManagerPlaybackTest.kt` next to it), `plugins/audio-playback/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-audio-playback`. The `stop()` half matches the existing KDoc and is a
plain bug fix; the restart half is a small **Decision** (see below).

## Problem

`MusicManager.stop()` is documented as "Stops playback ... and resets its position", but the implementation only acts
on a track that is audibly playing:

```kotlin
// MusicManagerImpl
override fun stop(uri: String) {
    if (isPlaying(uri)) {
        scope.launch {
            audioCache.loaded(uri)?.let { music -> musicPlayer?.stop(music) }
        }
    }
}
```

Every platform reports `isPlaying == false` for a paused track, so play → pause → stop → play resumes from the paused
position instead of the start, on all four platforms. The `test-audio` example's Stop button
(`examples/test-audio/.../AudioTestManager.kt`, `onStopClicked = { musicManager.stop(track1Uri) }`) shows it directly.

`play(uri, shouldRestart = true)` on a paused track is inconsistent across platforms: Desktop rewinds
(`DesktopMusicPlayer.play`: `if (shouldRestart) { cancelCurrentJob() }`), the others only rewind a playing track and
otherwise resume:

```kotlin
// MusicPlayer.android.kt
if (shouldRestart && isPlaying) { pause(); seekTo(0) }
// MusicPlayer.ios.kt
if (shouldRestart && cachedMusic.isPlaying()) { cachedMusic.stop(); cachedMusic.setCurrentTime(0.0) }
// WebMusicPlayer.play()
if (shouldRestart && isPlaying) { stopInternal(resetPosition = true) }
```

Consumers: no example and no Tesselar code passes `shouldRestart` (grepped; Tesselar does not use the audio plugin at
all); the games call `play(uri, shouldLoop = true)` and `pause(uri)`, which this plan does not change. Only
`test-audio`'s Stop button changes behaviour, to the documented one.

## Decision

What `play(shouldRestart = true)` does to a paused track:

- **(a) Rewind whenever `shouldRestart` is true (recommended)** — Desktop's current behaviour; "restart" reads as
  "from the beginning", and a caller who wants to resume passes `false` (the default). KDoc of `play`'s
  `shouldRestart` becomes "Whether to start the music from the beginning even if it is playing or paused."
- (b) Rewind only a playing track (the KDoc's literal wording) and change Desktop to resume a paused track instead.

## Fix

1. `MusicManagerImpl.stop()`: drop the `isPlaying` gate — stop whenever the track is loaded:
   `audioCache.loaded(uri)?.let { music -> musicPlayer?.let { player -> scope.launch { player.stop(music) } } }`.
2. Make every platform's `stop` safe on a loaded track that is paused or was never started:
   - Android: `MediaPlayer.pause()` is invalid in the `Prepared` state (moves the player to `Error`), so
     `stop` becomes `if (isPlaying) pause(); seekTo(0)` (`seekTo` is valid in Prepared, Started, Paused and
     PlaybackCompleted). `dispose` keeps its `runCatching { stop(cachedMusic) }`.
   - iOS: already rewinds unconditionally; no change beyond plan 16's TODO removal.
   - Web: `stop()` → `stopInternal(resetPosition = true)` already handles a paused player.
   - Desktop: `stop()` cancels the job, which also exists while paused; no change.
3. Restart (option a): Android `if (shouldRestart) { if (isPlaying) pause(); seekTo(0) }`; iOS
   `if (shouldRestart) { if (cachedMusic.isPlaying()) cachedMusic.stop(); cachedMusic.setCurrentTime(0.0) }`; Web
   `if (shouldRestart) stopInternal(resetPosition = true)` (safe while paused: it resets `pausedAt`, and the following
   branch starts a new job because `playJob` is null and `isPlaying` is false). Desktop unchanged. Update the KDoc.
   For option (b): leave those three alone and make Desktop's `play` rewind only when `isPlaying`.
4. Test seam: add a last constructor parameter to `MusicManagerImpl`,
   `private val initialMusicPlayer: MusicPlayer? = null` (the pattern of `GamepadInputManagerImpl`'s
   `initialGamepadEventHandler`). Extract the body of the `if (musicPlayer == null)` block in `Composable()` into
   `private fun attachMusicPlayer(player: MusicPlayer)`, and call it from a new
   `override fun onInitialize(kubriko: Kubriko)` when `initialMusicPlayer != null` (the `Composable()` check then
   finds `musicPlayer` set and skips). `MusicManagerImpl` is internal and `MusicManager.newInstance` is unchanged, so
   the public API is untouched.
5. `plugins/audio-playback/CLAUDE.md` MusicManager Internals: one bullet — `stop()` rewinds a paused or never-started
   track too; `shouldRestart` rewinds a paused track (if option a).

## Tests

In `plugins/audio-playback/src/desktopTest/kotlin/com/pandulapeter/kubriko/audioPlayback/`, a new
`MusicManagerPlaybackTest` with a `FakeMusicPlayer : MusicPlayer` (internal interface, reachable from desktopTest):
`preload` returns a token `Any()` per URI, `play` marks it playing, `pause` marks it not playing, `stop` increments an
`AtomicInteger` per token and marks it not playing; `isPlaying` reads the mark (use thread-safe fields: calls arrive on
the Kubriko scope's dispatcher). Build the manager with `MusicManagerImpl(isLoggingEnabled = false,
instanceNameForLogging = null, initialMusicPlayer = fake)` and `newManualKubriko(manager)` from `:tools:test-fixtures`
(a headless instance stays focused, so `play()` is not suppressed).

- `stopRewindsAPausedTrack`: `play(URI)`, `tickUntil { manager.isPlaying(URI) }`, `pause(URI)`, `stop(URI)`,
  `tickUntil { fake.stopCount(URI) == 1 }`. Fails today (stop is never forwarded).
- `stopReachesALoadedTrackThatNeverPlayed`: `preload(URI)`, `tickUntil { progress == 1f }` (via
  `getLoadingProgress(URI).first()` in `runBlocking`), `stop(URI)`, `tickUntil { fake.stopCount(URI) == 1 }`.
- `stopOfAnUnloadedTrackDoesNothing`: `stop(OTHER_URI)`, tick a few times, `fake.stopCount(OTHER_URI) == 0` and
  nothing was preloaded.

The per-platform rewinding cannot be unit-tested (no platform audio on the test classpath).

## Manual check

In the Showcase's Audio test on Android, iOS, Web and Desktop: Play track 1, Pause, Stop, Play — it must start from
the beginning. Stop on a loaded track that was never played must not break later playback (Android in particular).
