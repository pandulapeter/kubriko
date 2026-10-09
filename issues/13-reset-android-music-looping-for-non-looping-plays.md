# Reset Android music looping for non-looping plays

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Android
**Challenged:** sound
**Files:** `plugins/audio-playback/src/androidMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.android.kt`

Ships in `io.github.pandulapeter.kubriko:plugin-audio-playback`. No API change; aligns Android with the other three
platforms.

## Problem

The Android `play` only ever turns looping on:

```kotlin
if (shouldLoop) {
    isLooping = true
    setOnCompletionListener(null)
} else {
    setOnCompletionListener {
        pause()
        seekTo(0)
    }
}
```

Once a track has been played with `shouldLoop = true`, a later `play(uri, shouldLoop = false)` leaves the
`MediaPlayer` looping, and a looping `MediaPlayer` never reaches completion, so the track repeats forever. iOS
(`setNumberOfLoops(if (shouldLoop) NSIntegerMax else 0)`), Web (`sourceNode?.loop = shouldLoop`) and Desktop
(`this.shouldLoop = shouldLoop`) all apply the new value. The `test-audio` example's Loop toggle reproduces it.

## Fix

Set `isLooping = shouldLoop` unconditionally, before the `if`, and keep the listener branches:

```kotlin
isLooping = shouldLoop
if (shouldLoop) setOnCompletionListener(null) else setOnCompletionListener { pause(); seekTo(0) }
```

(The `// If shouldRestart is true ...` comment and the restart branch are handled by plan 12; if that plan landed
first, keep its version of the restart lines.)

## Tests

None: the code is `MediaPlayer` interop only and there are no Android unit tests in this module.

## Manual check

On Android, in the Showcase's Audio test: turn Loop on for track 1, Play, Pause, turn Loop off, Play — the track must
stop at its end instead of starting over.
