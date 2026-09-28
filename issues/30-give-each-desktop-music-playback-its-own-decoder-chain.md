# Give each desktop music playback job its own decoder chain, so a cancelled job can't tear down the one that replaced it

**Challenged:** sound

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** desktop
**Artifact:** `plugin-audio-playback` (internal change only, no public API touched)
**Files:** `plugins/audio-playback/src/desktopMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/DesktopMusicPlayer.kt`, `plugins/audio-playback/CLAUDE.md`

## Problem

`DesktopMusicPlayer` keeps one decoder chain (`decoder`, `audioDevice`, `bitstream`) and one `musicPlayingJob` in plain
fields, shared by every playback job. `stop()` cancels the job without waiting for it, clears the reference, and
rebuilds the chain:

```kotlin
fun stop() {
    // Cancel the decoding coroutine and rebuild the decoder/device so the next playback starts clean.
    musicPlayingJob?.cancel()
    musicPlayingJob = null
    isMusicPaused.value = false
    rebuildDecoderChain()
}
```

Cancellation is cooperative, so the old job carries on until its next `ensureActive()` (after the frame being
written, which blocks for about one MP3 frame). Then its `finally` runs, and it assumes it is still the current job:

```kotlin
} finally {
    // Make sure we are ready for the next invocation once the coroutine finishes.
    rebuildDecoderChain()
    isMusicPaused.value = false
    musicPlayingJob = null
}
```

`play(shouldRestart = true)` while a track is playing runs `stop()` and then `startPlayback()` in one call, so this
happens **every time**, as it does for a quick stop-then-play:

1. The old job's `finally` rebuilds the chain under the new job: the new job's next `playFrame()` reads a fresh
   `bitstream` from byte 0 with a new `decoder`, so playback jumps and can decode garbage for a frame.
2. It sets `musicPlayingJob = null` while the new job is running: `isPlaying` reports `false` for music that is
   audibly playing, `MusicManager.play()` (which checks `!isPlaying(uri)`) then starts a **second** job decoding
   from the same shared `bitstream` into the same device (garbled audio), and `stop()` can no longer reach either
   job (`musicPlayingJob?.cancel()` on `null`): the music can't be stopped until the manager is disposed.

On top of that, `play`, `pause` and `stop` are called from coroutines that `MusicManagerImpl` launches on
`Dispatchers.Default`, so two of them can run in parallel and both see `musicPlayingJob == null`. `stop()` also
rebuilds (closes) the device while the old job may still be inside `playFrame()` writing to it.

Separately, `startPlayback()` never clears `isMusicPaused`, so a `pause()` that lands while no job is running makes
the next job start suspended.

## Fix

Make the chain local to the job that uses it, and make the job reference changes atomic.

1. Delete the `decoder`, `audioDevice` and `bitstream` fields, `init { rebuildDecoderChain() }` and
   `rebuildDecoderChain()`. Inside `startPlayback`'s coroutine, create the `Decoder`, the `AudioDevice`
   (`FactoryRegistry.systemRegistry().createAudioDevice().also { it.open(decoder) }`) and the `Bitstream` as
   locals (inside the `try`, so a `JavaLayerException` from device creation ends the job instead of escaping into
   the Kubriko scope). `playFrame` and the loop rewind take them as parameters (the loop rewind closes and recreates
   only the local bitstream, as `rewindBitstreamOnly()` does today). Nothing is opened while no job runs; JLayer's
   `JavaSoundAudioDevice` only acquires the line on the first write, so moving creation to play time costs nothing
   noticeable.
2. Guard the job reference with a private lock (`private val lock = Any()`, JVM `synchronized`) and mark
   `musicPlayingJob` and `shouldLoop` `@Volatile` for the unsynchronised reads (`isPlaying`, the loop condition):
   - `play()`: under the lock, set `shouldLoop`; if `shouldRestart`, cancel the current job (see 3); if no job,
     set `isMusicPaused.value = false` and start one; otherwise resume (`isMusicPaused.value = false`).
   - `stop()`: under the lock, cancel the current job, `musicPlayingJob = null`, `isMusicPaused.value = false`.
     It no longer touches any device.
   - `pause()` unchanged.
3. The job's `finally` closes **its own** chain, then, under the lock, clears the shared state only if it is still
   the current job:
   ```kotlin
   synchronized(lock) {
       if (musicPlayingJob === coroutineContext.job) {
           musicPlayingJob = null
           isMusicPaused.value = false
       }
   }
   ```
   Close the device with `flush()` (which drains the line) only when the track ended on its own; when the job was
   cancelled, `close()` it without flushing, so a stop or a restart cuts the old audio instead of playing out its
   buffer on top of the new job.
4. `dispose()` becomes `stop()`: the cancelled job releases its own chain. Keep `setVolume` and `applyVolume` as they
   are.

Replace the class-level comments that describe the removed rebuild behaviour; keep the existing comment on
`isMusicPaused`.

## Tests

None. Every path runs through a JLayer `AudioDevice` backed by a JavaSound line, which a headless test JVM can't
open (the write fails and the job ends at once), so the job hand-over can't be observed from a unit test.

## Manual check

Desktop (`./gradlew :app:desktop:run` with `showcase.areTestExamplesEnabled=true`), the Audio test:
1. Play track 1, press Stop and Play again as fast as possible ten times: exactly one copy of the track plays each
   time, from the start, without a burst of noise, and the play/pause icon always matches what is heard.
2. Play track 1, then Stop: the music stops immediately (no half-second tail).
3. In any game that restarts its music (`play(uri, shouldRestart = true)` while playing), restart several times:
   one clean copy of the track each time, and pausing (switching windows) silences it.

Update the Desktop line of `plugins/audio-playback/CLAUDE.md` → Gotchas (`rebuildDecoderChain()` recreates
`Decoder + AudioDevice` on every `stop()`/restart) to say each playback job builds its own decoder chain and closes
it when it ends.
