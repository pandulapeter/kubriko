# Move the loading-progress flow duplicated by `MusicManagerImpl` and `SoundManagerImpl` into `AudioCache`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-audio-playback
**Files:**
- `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/AudioCache.kt`
- `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/MusicManagerImpl.kt`
- `plugins/audio-playback/src/commonMain/kotlin/com/pandulapeter/kubriko/audioPlayback/SoundManagerImpl.kt`
- `plugins/audio-playback/src/desktopTest/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/AudioCacheTest.kt`

## Problem
The identical expression appears in MusicManagerImpl.kt:85–87 and SoundManagerImpl.kt:65–67 at 2480325f:

```kotlin
override fun getLoadingProgress(uris: Collection<String>) = if (uris.isEmpty()) flowOf(1f) else audioCache.entries.map { cache ->
    cache.filter { (key, _) -> key in uris }.count { (_, value) -> value != null }.toFloat() / uris.size
}.distinctUntilChanged()
```

It is a pure function of `AudioCache.entries`, which is where it belongs; a fix to one copy would miss the other.

## Fix
- Add to `AudioCache`, below `entries`:

  ```kotlin
  /** The share of [uris] that are settled (loaded or failed), from 0 to 1; 1 for an empty collection. */
  fun loadingProgress(uris: Collection<String>): Flow<Float> = if (uris.isEmpty()) flowOf(1f) else entries.map { cache ->
      cache.filter { (key, _) -> key in uris }.count { (_, value) -> value != null }.toFloat() / uris.size
  }.distinctUntilChanged()
  ```

  (the expression verbatim, `audioCache.entries` → `entries`; `AudioCache` is `internal`, so the function is too).
- Both managers: `override fun getLoadingProgress(uris: Collection<String>) = audioCache.loadingProgress(uris)`.
  Keep the declared return type the manager's abstract function has. Trim `flowOf`, `map`, `distinctUntilChanged`
  imports from the managers where now unused; add `flowOf`/`distinctUntilChanged` to `AudioCache`.
- Leave the two `load()` logging wrappers alone (they take different player types; merging them needs a shared
  interface, out of scope).

## Behaviour
Unchanged: the same expression over the same flow.

## Public API
None.

## Tests
Add one test to `AudioCacheTest` in the style of its existing tests: with two URIs preloaded and a loader that
completes only one, `loadingProgress(setOf(a, b)).first { it == 0.5f }` arrives, and `loadingProgress(emptySet())`
emits `1f`. Use the test's existing scope/loader helpers.

## Verify
`./gradlew :plugins:audio-playback:compileKotlinDesktop :plugins:audio-playback:desktopTest`

## Manual check
none
