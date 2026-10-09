# Correct the iOS music stop documentation

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** iOS
**Challenged:** sound
**Files:** `plugins/audio-playback/CLAUDE.md`, `plugins/audio-playback/src/iosMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.ios.kt`

Simple (docs and a stale comment). No published behaviour changes.

## Problem

`plugins/audio-playback/CLAUDE.md` says twice that iOS music `stop()` only pauses:

- Platform Backends table, iOS row: "`stop()` effectively pauses (known TODO)"
- Gotchas: "iOS: `stop()` is actually a pause — the playback position is preserved"

and `MusicPlayer.ios.kt` still carries the matching TODO, although the code below it rewinds:

```kotlin
// TODO: Works like a pause, not a stop
override fun stop(cachedMusic: Any) {
    cachedMusic as AVAudioPlayer
    if (cachedMusic.isPlaying()) {
        cachedMusic.stop()
    }
    cachedMusic.setCurrentTime(0.0) // Reset to beginning
}
```

(`AVAudioPlayer.stop()` keeps `currentTime`; the explicit `setCurrentTime(0.0)` is what makes this a real stop.) An
agent reading the notes would "fix" a non-existent iOS bug or work around it. The remaining reason a stop could look
like a pause — `MusicManagerImpl.stop()` ignoring paused tracks on every platform — is plan 12, not an iOS quirk.

## Fix

1. Delete the `// TODO: Works like a pause, not a stop` line in `MusicPlayer.ios.kt`.
2. In `plugins/audio-playback/CLAUDE.md`, remove "`stop()` effectively pauses (known TODO)" from the iOS row's Notes
   (leave the cell empty or keep only what plan 11 puts there) and delete the "iOS: `stop()` is actually a pause"
   Gotchas bullet.

If plan 11 lands first it rewrites the same iOS row: keep its Music column text and only drop the stop note.

## Tests

The existing ones.

## Manual check

None.
