# Apply maximumSimultaneousStreamsOfTheSameSound per sound on Android, as documented, instead of as the SoundPool's total

**Challenged:** sound

**Decision needed:** Should Android honour the KDoc ("the maximum number of times the same sound can be played
simultaneously") by limiting streams per sound, which lets more *different* sounds play at once than today? —
recommended: **yes, limit per sound and give the pool a fixed total**.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Android
**Artifact:** `plugin-audio-playback` — observable on Android (how many sounds are heard at once); no signature change
**Files:** `plugins/audio-playback/src/androidMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/SoundPlayer.android.kt`, `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/SoundManager.kt` (option B only), `plugins/audio-playback/CLAUDE.md`

## Problem

`SoundManager.newInstance` documents
`@param maximumSimultaneousStreamsOfTheSameSound The maximum number of times the same sound can be played
simultaneously`, and desktop (a `Clip` pool per sound), iOS and web (a list of players per sound) implement exactly
that. Android passes it to `SoundPool.Builder().setMaxStreams(...)`:

```kotlin
SoundPool.Builder()
    .setMaxStreams(maximumSimultaneousStreamsOfTheSameSound)
```

`setMaxStreams` is the pool's **total** across all sounds. With the default of 5, an Android game can play at most
five sound effects of any kind at once; the sixth steals the stream of the lowest-priority (all are priority 1, so
the oldest) one, cutting it off. A shooter with an engine loop, gunfire and explosions loses sounds on Android that
play fine on the other three platforms.

## Fix

**Option A (recommended): limit per sound.**
- Build the pool with a fixed total, `setMaxStreams(MAXIMUM_TOTAL_STREAMS)` with
  `private const val MAXIMUM_TOTAL_STREAMS = 32` (well within what the mixer gives one process; a larger value only
  reserves bookkeeping in `SoundPool`, not audio tracks).
- Keep, per loaded sample, a ring of its last `maximumSimultaneousStreamsOfTheSameSound` stream ids
  (`HashMap<Int, IntArray>` plus a cursor per sample, created when the sample loads, so `play` doesn't allocate).
  In `play`: if the ring slot about to be reused holds a stream id, `soundPool.stop(thatId)` (stopping a finished
  stream is a no-op), then store the id `soundPool.play(...)` returns. `dispose(cachedSound)` drops the sample's ring.
  `SoundPool` reports no end-of-stream, so a busy slot can't be told from a finished one; stealing the oldest is
  therefore the Android behaviour, where desktop drops the new sound — state that in CLAUDE.md.
- Access the rings from one thread: `play` currently runs in `withContext(Dispatchers.Default)`; either keep it there
  and guard the map with `synchronized`, or switch `play` to `Dispatchers.Main` (plan 32 already keeps the load map
  on main). Recommended: `synchronized` on the map, so a burst of sounds doesn't queue behind the UI thread.

**Option B: document the platform difference.** Leave the code; change the KDoc of
`maximumSimultaneousStreamsOfTheSameSound` to say that on Android it caps the total number of sound effects playing
at once, and update CLAUDE.md. Keeps Android's behaviour identical for existing games.

## Tests

None: `SoundPool` exists only on a device. The ring is a few lines inside `play`; extracting it for a JVM test would
add an abstraction for no other use.

## Manual check

Android device, a test build that plays six different sounds within 100 ms (e.g. the Wallbreaker sounds triggered
from a debug button), with the default `maximumSimultaneousStreamsOfTheSameSound = 5`: under option A all six are
heard to the end; playing the *same* sound six times quickly cuts off only the first. Under option B nothing changes.

Update `plugins/audio-playback/CLAUDE.md` → SoundManager Internals: the first bullet says the parameter "controls
SoundPool streams on Android" — replace it with the per-sound ring (A) or the total cap (B).
