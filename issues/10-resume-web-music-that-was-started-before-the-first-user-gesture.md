# Resume web music that was started before the first user gesture

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Web
**Challenged:** amended — listeners must be removed with the same capture flag they were added with, every `resume()` promise gets a rejection handler (closing a context rejects a pending resume), and the note that a freshly built context can report `"suspended"` while already allowed to start.
**Files:** `plugins/audio-playback/src/webMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/WebMusicPlayer.kt`, a new `plugins/audio-playback/src/webMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/WebAudioUnlocker.kt`, `plugins/audio-playback/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-audio-playback`. No public API change.

## Problem

Browsers' autoplay policy creates an `AudioContext` in the `"suspended"` state when the page has not yet received a
user gesture, and such a context stays silent until `resume()` is called after one. `WebMusicPlayer` builds a fresh
`AudioContext` for every play and never checks or resumes it:

```kotlin
// WebMusicPlayer.play()
if (playJob == null && !isPlaying) {
    playJob = scope.launch(Dispatchers.Default) {
        rebuildAudioGraph(shouldLoop)
        sourceNode?.start(0.0, pausedAt)
        startedAt = (audioContext?.currentTime ?: 0.0) - pausedAt
        isPlaying = true
    }
}
```

```kotlin
// WebMusicPlayer.rebuildAudioGraph()
val context = AudioContext().also { audioContext = it }
```

`isPlaying` becomes `true` although nothing is audible, so `MusicManagerImpl.play()` (`if (shouldRestart ||
!isPlaying(uri))`) skips every later call, and the track never starts — not after the user clicks either. Every game
in `examples/` calls `musicManager.play(...)` as soon as `stateManager.isFocused` turns true, so any web page that
opens straight into a game (the Showcase's deep links, `app/web/.../KubrikoShowcaseApp.kt` reads
`window.location.pathname`; or any consumer game whose first screen plays music) stays silent until the track is
paused and played again. In the Showcase's own menu flow the first click happens to come before the first `play()`,
which is why this is not seen there.

## Fix

1. Add the missing members to the `AudioContext` external declaration at the bottom of `WebMusicPlayer.kt`:
   `val state: String` and `fun resume(): Promise<JsAny?>`.
2. Add an `internal object WebAudioUnlocker` (own file, MPL header) that keeps a list of contexts waiting for a
   gesture. `register(context)` adds the context and, when the list was empty, adds listeners on `window` (capture
   phase, `addEventListener(type, listener, true)`) for the activation-triggering events `pointerdown`, `pointerup`, `keydown` and `touchend` (per the HTML
   spec, a touch `pointerdown` does not grant activation, a `pointerup`/`touchend` does, so all four are needed). Each
   event calls `resume()` on every registered context whose `state` is still `"suspended"` and drops the ones that
   are `"running"` or `"closed"`; when the list is empty the listeners are removed. Keep the listener lambdas as
   properties so the same references are removed, and remove them with the same capture flag
   (`removeEventListener(type, listener, true)`): the flag is part of a listener's identity, and a removal without it
   leaves the listener attached. `unregister(context)` drops a context (called from teardown).
3. In `WebMusicPlayer`, right after `sourceNode?.start(...)`: if `context.state == "suspended"`, call
   `context.resume()` once (it succeeds outright when the page already has sticky activation) and
   `WebAudioUnlocker.register(context)`. Every `resume()` call (here and in the unlocker) gets a rejection handler,
   `context.resume().catch { null }`: `stopInternal()` closes the context, which rejects a `resume()` still pending
   (a pause before the first gesture does exactly that), and an unhandled rejection is an "Uncaught (in promise)"
   console error. A newly constructed context may also report `"suspended"` while it is already allowed to start
   (per the spec its state turns `"running"` only once rendering starts; Firefox reports it that way), so this path
   also runs on ordinary plays; that is harmless — the next gesture or the next stop drops the context. In `stopInternal()`, call `WebAudioUnlocker.unregister(it)` before
   `it.close()`.
4. Keep `isPlaying` meaning "playback has been started and not paused or stopped". Reporting `false` while the context
   is suspended was considered and rejected: `MusicManagerImpl.pause()` is gated on `isPlaying`, so the focus-loss
   pause would then be skipped for a pending track and the unlocker would start it while the game is paused; and
   reading `state` from `isPlaying` would cross the JS boundary every frame for games that poll `isPlaying()` in
   `onUpdate` (the `test-audio` example does).
5. Not in this plan: replacing the per-play `AudioContext` with one long-lived context per player. It would still
   need the unlocker, and it is a larger restructuring of pause/resume.

Update the Web row of the Platform Backends table in `plugins/audio-playback/CLAUDE.md`: music started before the
first user gesture is resumed on the first gesture.

## Tests

None: the Wasm test tasks are disabled and the code is browser interop only.

## Manual check

Build the Showcase for the web (`./gradlew :app:web:wasmJsBrowserDevelopmentRun`), open a game's deep link (for
example Space Squadron's path) in a fresh tab without clicking anything, confirm there is no music, then click once on
the page: the music must start. Also check that pausing (switching tabs) before the first click and then clicking does
not leave music playing over a paused game, and that the normal menu flow still plays music on Chrome and Firefox.
