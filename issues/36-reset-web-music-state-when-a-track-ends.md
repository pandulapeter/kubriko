# Reset the web music player's state when a non-looping track ends, and keep the pause position inside the track

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** web
**Artifact:** `plugin-audio-playback` (internal; brings web in line with the documented `isPlaying` and the other three platforms)
**Files:** `plugins/audio-playback/src/webMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/WebMusicPlayer.kt`

## Problem

`WebMusicPlayer` sets `isPlaying = true` when it starts a source node and only ever resets it in `stopInternal()`:

```kotlin
playJob = scope.launch(Dispatchers.Default) {
    rebuildAudioGraph(shouldLoop)
    sourceNode?.start(0.0, pausedAt)
    startedAt = (audioContext?.currentTime ?: 0.0) - pausedAt
    isPlaying = true
}
```

Nothing listens for the source node's `ended` event, so after a track played with `shouldLoop = false` finishes:

- `isPlaying` stays `true` and `playJob` stays non-null forever. `MusicManager.isPlaying(uri)` reports a silent
  track as playing, and `MusicManager.play(uri)` — which only acts when `shouldRestart || !isPlaying(uri)` — does
  **nothing**, so a jingle can be played exactly once per load on the web (Android, desktop and iOS all report
  `false` at the end and replay it).
- The graph's `AudioContext` stays open until a later stop.
- A `pause()` after the end computes `pausedAt = currentTime - startedAt`, past the track's duration; the next play
  starts at that offset and ends at once.

For looping tracks `pausedAt` has the same problem: after the first loop, `currentTime - startedAt` exceeds the
duration, and resuming passes an offset beyond the buffer to `start()`.

## Fix

1. Declare what's needed on the externals: `val duration: Double` on `AudioBuffer`, and
   `var onended: ((JsAny) -> Unit)?` on `AudioBufferSourceNode` (the same shape `kotlinx-browser` uses for event
   handler properties on Wasm; if the compiler rejects it, declare
   `fun addEventListener(type: String, callback: (JsAny) -> Unit)` on `AudioNode` instead).
2. In `rebuildAudioGraph`, after creating `source`:
   ```kotlin
   source.onended = {
       if (sourceNode === source) {
           stopInternal(resetPosition = true)
       }
   }
   ```
   The identity check is what keeps the `ended` event that `stopInternal()` itself triggers (via `source.stop()`) —
   and the one of any graph replaced by a restart — from tearing down the graph that is current by then:
   `stopInternal` sets `sourceNode = null` before that event is delivered. A looping source only fires `ended` when
   stopped, so it needs no special case. `stopInternal(resetPosition = true)` already clears `isPlaying`, `playJob`
   and `pausedAt` and closes the context.
3. In `pause()`, wrap the position into the buffer:
   ```kotlin
   val elapsed = (audioContext?.currentTime ?: 0.0) - startedAt
   val duration = audioBuffer?.duration ?: 0.0
   pausedAt = if (duration > 0.0) elapsed % duration else 0.0
   ```

No other change: the Wasm target is single-threaded, so the handler can't race the rest of the class.

## Tests

None: the Web Audio API only exists in a browser, and the plugin has no browser test setup.

## Manual check

`./gradlew :app:web:wasmJsBrowserDevelopmentRun`, a game (or the Audio test changed locally to call
`play(uri, shouldLoop = false)`) with a short non-looping track:
1. Let it finish: the play/pause icon switches back to "play" (`isPlaying` is false).
2. Press play again: it plays again from the start.
3. With a looping track, let it loop at least once, pause, resume: playback resumes where it was paused, not at the
   start or in silence.
4. DevTools → the WebAudio panel (Chrome) shows the playback context closed after the track ends.
