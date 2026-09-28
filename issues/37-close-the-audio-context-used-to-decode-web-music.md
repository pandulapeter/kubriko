# Close the AudioContext that decodes each web music file once the decode is done

**Challenged:** sound

**Kind:** bug (resource leak)  ·  **Severity:** low  ·  **Platforms:** web
**Artifact:** `plugin-audio-playback` (internal change only)
**Files:** `plugins/audio-playback/src/webMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/WebMusicPlayer.kt`

## Problem

Every music preload creates an `AudioContext` just to call `decodeAudioData` on it, and never closes it:

```kotlin
audioBuffer = AudioContext().decodeAudioData(arrayBuffer).await()
```

(After plan 34 this line lives in the suspend `load` factory; find it by `decodeAudioData`.) An `AudioContext`
holds an audio output stream and a rendering thread until it is closed or garbage-collected, and browsers cap how
many can be live (Chrome logs a warning and, on some platforms, refuses new ones past a small number; Safari has
historically limited it to four). A game with several tracks, or the Showcase switching between games (each with
its own `MusicManager` preloading again), accumulates contexts; the one created for playback in `rebuildAudioGraph`
can then fail to start.

## Fix

Close the decoding context as soon as the buffer is decoded (the decoded `AudioBuffer` does not depend on it):

```kotlin
val decodingContext = AudioContext()
try {
    audioBuffer = decodingContext.decodeAudioData(arrayBuffer).await()
} finally {
    decodingContext.close()
}
```

`close()` returns a promise; the external declares it as `fun close()`, which is fine — nothing needs to wait for
it. (An `OfflineAudioContext` would avoid the output stream entirely, but needs a length and sample rate up front; not
worth it here.)

## Tests

None: requires a browser's Web Audio implementation.

## Manual check

Chrome, web Showcase: open and leave the music-playing games (Wallbreaker, Space Squadron, Blocky's Journey) several
times in a row. DevTools → More tools → WebAudio lists the live contexts: after the loads finish, only the contexts
of tracks that are currently playing remain (before the fix, one extra per loaded track, growing with every visit).
