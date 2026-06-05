<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# test-audio

Test example that exercises the `audio-playback` plugin (`MusicManager` + `SoundManager`).
Only enabled in the Showcase app when `showcase.areTestExamplesEnabled=true` in `gradle.properties`.
Audio playback works on Android and Desktop (JVM) only.

## What it tests

- Streaming music playback via `MusicManager`: play, pause, stop, and looping for two MP3 tracks
- Resource URI construction across platforms (web requires special path rewriting — see `ResourceLoader.kt`)
- Loading-progress reporting (`musicManager.getLoadingProgress(uri)`) with a `LoadingIndicator` while tracks load
- Graceful stop-before-dispose: `stopMusicBeforeDispose()` must be called before `kubriko.dispose()` to avoid
  audible cutoffs during the cross-fade navigation transition in the Showcase app

## Module structure

```
AudioTest.kt                        — public Composable + createAudioTestStateHolder() factory
implementation/
  AudioTestStateHolder.kt           — sealed interface + Impl; creates MusicManager, SoundManager, AudioTestManager
  managers/
    AudioTestManager.kt             — Manager: preloads tracks, updates play-state every tick, renders controls UI
  utilities/
    ResourceLoader.kt               — expect/actual; constructs a URI for a file in composeResources/files/music/
    ResourceLoader.android.kt / .desktop.kt / .ios.kt / .web.kt
```

## Key patterns

- `AudioTestStateHolder` extends the shared `StateHolder` interface from `examples/shared`.
- `AudioTestStateHolderImpl` creates the Kubriko instance with `MusicManager`, `SoundManager`, and `AudioTestManager`.
- `AudioTestManager` extends `Manager` and overrides `Composable(windowInsets)` to render playback controls
  directly inside the Kubriko viewport overlay — no separate Compose layer needed for UI.
- `isTrack1Playing` / `isTrack2Playing` are `mutableStateOf` updated in `onUpdate()` each tick to drive
  the play/pause button icon reactively. **Do not capture these in lambdas from hot paths.**
- The `webRootPathName` parameter threads through `StateHolder` → `Manager` → `ResourceLoader` to handle
  the Showcase web app's path-prefix quirk; normal apps can pass `""`.
- `stopMusicBeforeDispose()` sets a `MutableStateFlow<Boolean>` flag; a coroutine in `onInitialize` reacts
  to it by stopping both tracks. The flag is cleared when the viewport regains focus.

## Resource files

Music tracks live under `src/commonMain/composeResources/files/music/` as MP3 files.
The URI constants in `AudioTestManager` reference these by relative path (`files/music/<name>.mp3`).
