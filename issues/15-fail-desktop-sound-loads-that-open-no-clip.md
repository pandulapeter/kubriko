# Fail desktop sound loads that open no clip

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Desktop
**Challenged:** amended — a system with no Clip line at all (no audio device) keeps today's empty, silently dropping `CachedSound` instead of a failed load, because `AudioCache` retries a failed sound on every `play()` and would re-read, re-decode and (with logging on) log at HIGH once per sound effect played; the test branches on the same check.
**Files:** `plugins/audio-playback/src/desktopMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/CachedSound.kt`, `plugins/audio-playback/src/desktopMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/SoundPlayer.desktop.kt`, a new `plugins/audio-playback/src/desktopTest/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/CachedSoundTest.kt`, `plugins/audio-playback/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-audio-playback`. No API change.

## Problem

`CachedSound` opens its clips in `init` and swallows every failure:

```kotlin
init {
    repeat(maxSimultaneousStreams) { createAndAddClip() }
}

private fun createAndAddClip(): Clip? {
    return try {
        val clip = AudioSystem.getClip()
        ...
        clip.open(stream)
        ...
    } catch (e: Exception) {
        null
    }
}
```

When no clip opens — a format the system mixer rejects (verified on macOS with a JDK 21 probe: a 9-channel
`ULAW` format and a 10 MHz PCM format throw `LineUnavailableException` from `Clip.open`), no audio device
(`AudioSystem.getClip()` throws), or the mixer running out of lines because every preloaded sound holds
`maximumSimultaneousStreamsOfTheSameSound` open lines — `SoundPlayer.desktop.kt`'s `preload` still returns the
`CachedSound`. The sound counts as loaded, every `play()` finds an empty pool and is silently dropped, nothing is
logged, and it is never retried: exactly the case the `AudioCache` failure path (`null` → log at `HIGH`, retry on the
next `preload()`/`play()`) exists for.

## Fix

1. Give `CachedSound` a way to report an empty pool without changing its construction path, e.g. a factory in its
   companion: `fun create(audioData: ByteArray, audioFormat: AudioFormat, maxSimultaneousStreams: Int): CachedSound?`
   that constructs it and returns `null` (after `dispose()`) when `clipPool` is empty; make the constructor private.
   Exception: when the system has no Clip line at all (`!AudioSystem.isLineSupported(Line.Info(Clip::class.java))`,
   no audio device), return the empty `CachedSound` as today. A failed load is retried by `AudioCache` on every
   `play()` of that sound, so on a device-less machine every sound effect played would re-open and re-decode its file
   on `Dispatchers.IO`, allocate its whole `ByteArray` again and, with logging on, log "Failed to load" at `HIGH` —
   dozens of times a second in a shooter — to report something that cannot be fixed by retrying and that nobody can
   hear anyway. A format the mixer rejects (or exhausted mixer lines) still fails and retries, like a missing file
   does today.
2. `SoundPlayer.desktop.kt` `preload`: use `CachedSound.create(...)`; when it returns `null`, return `null` (closing
   the streams as today — move the `close()` calls into a `finally`/`use` so they also run on this path).
3. Partial pools (some clips opened) stay accepted, as today: the sound plays with fewer simultaneous streams.
4. Not in this plan: opening clips lazily on first use to hold fewer mixer lines. It would add latency to the first
   play of each sound, the thing `SoundManager` is for; raise it separately if line exhaustion is seen in practice.
5. `plugins/audio-playback/CLAUDE.md` SoundManager Internals: one bullet — on Desktop a sound none of whose clips
   opens is a failed load (logged and retried), except on a system with no Clip line at all, where it
   stays loaded and its plays are dropped.

## Tests

`CachedSoundTest` in `plugins/audio-playback/src/desktopTest/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/`:

- `soundWhoseFormatNoClipAcceptsIsAFailedLoad`: `CachedSound.create(ByteArray(9 * 64),
  AudioFormat(AudioFormat.Encoding.ULAW, 8000f, 8, 9, 9, 8000f, false), maxSimultaneousStreams = 3)`. When
  `AudioSystem.isLineSupported(Line.Info(Clip::class.java))` is true (a machine with audio; verified on macOS / JDK
  21: true, and `Clip.open` throws `LineUnavailableException` for this format) it returns `null`; otherwise (a CI
  runner without a device) it returns a non-null sound whose `getAvailableClip()` is `null`. Both branches fail before
  the fix (no `create`; the constructor never returns `null`) and are deterministic for the machine they run on. It
  only touches `javax.sound`, no Skia.

A positive test (a valid format opens clips) is not written: it depends on the runner having an audio device.

## Manual check

On Desktop, preload a WAV the mixer rejects (e.g. a 24-bit 192 kHz or 8-channel file) with logging enabled on the
`SoundManager`: a `HIGH` "Failed to load" log must appear, and the game's other sounds must keep playing.
