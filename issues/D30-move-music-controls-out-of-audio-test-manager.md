# Move `MusicControls` and `ControlButton` out of `AudioTestManager` into `ui/MusicControls.kt`, taking state and callbacks.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/managers/AudioTestManager.kt`, `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/ui/MusicControls.kt` (new), `examples/test-audio/CLAUDE.md`
**Challenged:** amended — the play/pause click decides from `isTrackNPlaying.value` (the displayed state, as today) instead of a live `musicManager.isPlaying(uri)` read, which could diverge while the instance is not ticking.

## Problem
- `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/managers/AudioTestManager.kt` hosts two private member Composables, `MusicControls(title, musicUri, isPlaying)` (:129–171) and `ControlButton(...)` (:173–187). `MusicControls` reaches into the Manager: `musicManager.getLoadingProgress(musicUri).collectAsState(0f).value == 1f` (:148) and `musicManager.pause/play/stop(musicUri)` in its click handlers.
- `getLoadingProgress(uri)` builds a new `Flow` on every call (`MusicManagerImpl.kt:83-87`: `getLoadingProgress(setOf(uri))` → `audioCache.entries.map { … }.distinctUntilChanged()`), so every recomposition of a track's controls (each play/pause change) restarts the collector.
- `private var isTrack1Playing = mutableStateOf(false)` / `isTrack2Playing` (:74–75) are never reassigned.

## Fix
1. Create `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/ui/MusicControls.kt` (license header) with `@Composable internal fun MusicControls(title: String, isLoaded: Boolean, isPlaying: Boolean, onPlayPauseClicked: () -> Unit, onStopClicked: () -> Unit)` — the old body verbatim (`Panel { Column(padding 8, spacedBy 4) { Text; Row(CenterVertically, spacedBy 8) { … } } }`), with `if (isLoaded)` replacing the progress check, `onClick = onPlayPauseClicked` and `onClick = onStopClicked`. Keep the third, disabled loop `ControlButton(icon = Res.drawable.ic_loop_on, contentDescription = Res.string.loop_on, isEnabled = false, onClick = {})` exactly as it is (planned D52 decides its fate). Move `ControlButton` into the same file as `private`, verbatim.
2. In the Manager: `private val isTrack1Playing = mutableStateOf(false)` / `isTrack2Playing` (`val`); add `private fun togglePlayback(uri: String, isPlaying: Boolean) = if (isPlaying) musicManager.pause(uri) else musicManager.play(uri)`; in the `Composable` override call
   ```kotlin
   MusicControls(
       title = stringResource(Res.string.music_track_1),
       isLoaded = remember { musicManager.getLoadingProgress(track1Uri) }.collectAsState(0f).value == 1f,
       isPlaying = isTrack1Playing.value,
       onPlayPauseClicked = { togglePlayback(track1Uri, isTrack1Playing.value) },
       onStopClicked = { musicManager.stop(track1Uri) },
   )
   ```
   and the same for track 2. Trim imports (Image, clickable, RoundedCornerShape, alpha, clip, ColorFilter, LocalContentColor, DrawableResource, StringResource, painterResource, the icon/loop/play/pause/stop accessors).
3. `examples/test-audio/CLAUDE.md`: the tree line "`AudioTestManager.kt — Manager: preloads tracks, updates play-state every tick, renders controls UI`" becomes "…its `Composable` override lays out `ui/MusicControls` per track", add a `ui/MusicControls.kt` line, and in "Key patterns" keep the `isTrack1Playing`/`isTrack2Playing` sentence.

## Behaviour
Same tree. The play/pause decision reads `isTrackNPlaying.value` — the state the icon is drawn from, as before (not `musicManager.isPlaying(uri)`, which can differ from the drawn icon while the instance is not ticking, e.g. unfocused, since only `onUpdate` copies it). Loading progress is collected once per track instead of restarting on each recomposition; `produceState`'s value holder already survived restarts, so the loading indicator behaves the same.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:test-audio:compileKotlinDesktop` and `./gradlew :examples:test-audio:compileAndroidMain`

## Manual check
Desktop or Android, test examples enabled: both tracks show a loading indicator then play/pause/stop correctly; stop greys out while stopped.
