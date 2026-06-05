<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-audio-playback

Streaming music and pooled SFX. Reliably works on Android and Desktop; iOS and Web have limitations.

## Key Files

- `src/commonMain/.../MusicManager.kt` / `MusicManagerImpl.kt` — streaming audio with volume/loop control
- `src/commonMain/.../SoundManager.kt` / `SoundManagerImpl.kt` — low-latency SFX with pooling
- `src/commonMain/.../implementation/MusicPlayer.kt` — `expect` interface, one `actual` per platform
- `src/commonMain/.../implementation/SoundPlayer.kt` — `expect` interface, one `actual` per platform

## Critical: Players Are Created in `Composable()`, Not `onInitialize()`

`MusicPlayer`/`SoundPlayer` instances are created inside `Manager.Composable()`, meaning **audio is unavailable until the first composition of `KubrikoViewport`**. Call `preload()` early to hide loading latency; `play()` before composition queues a deferred load.

## Platform Backends

| Platform | Music | SFX | Notes |
|---|---|---|---|
| Android | `MediaPlayer` | `SoundPool` | `musicPauseDelayOnFocusLoss = 0` |
| Desktop | JLayer (MP3 via `libs.jlayer`), `javax.sound.sampled` | `Clip` pool | Entire file buffered into `ByteArray` at load; volume applied by scaling `SampleBuffer` samples per frame |
| iOS | `AVAudioPlayer` + `AVAudioSessionCategoryPlayback` | `AVAudioPlayer` clones | `stop()` effectively pauses (known TODO) |
| Web | Web Audio API / `AudioContext` | `HTMLAudioElement` pool | `musicPauseDelayOnFocusLoss = 100 ms`; Chrome Android bug: `dispose()` may not stop music — a 250 ms deferred second `dispose()` is used as workaround |

## MusicManager Internals

- Cache is `MutableStateFlow<PersistentMap<String, Any?>>` — `null` = loading in progress
- `play()` checks `stateManager.isFocused.value` first; silently suppressed when unfocused
- On focus loss, music is paused via `debounce(musicPauseDelayOnFocusLoss)`. Focus regain does **not** auto-resume — the game must call `play()` again
- `setVolume(uri, volume)` stores per-URI volume; applied just before each `play()` call
- `unloadAll()` clears cache but does not dispose the manager; `play()` after it triggers a fresh load

## SoundManager Internals

- `maximumSimultaneousStreamsOfTheSameSound` (default 5): controls SoundPool streams on Android, pre-created `Clip` count on Desktop, `AVAudioPlayer`/`HTMLAudioElement` clone count on iOS/Web
- Desktop clips return to pool via `LineEvent.Type.STOP`. If all clips are busy, the sound is **silently dropped** (no queuing)
- `play()` checks `stateManager.isFocused.value` — suppressed when unfocused
- No per-call volume or loop control in SFX; use `MusicManager` for those features

## Neither Manager Overrides `onUpdate`

All logic is coroutine- or callback-driven. Zero per-frame cost.

## Gotchas

- Audio formats: SFX → WAV (max 48k bitrate on Android); Music → MP3 (max 320 kbps)
- Desktop: `rebuildDecoderChain()` recreates `Decoder + AudioDevice` on every `stop()`/restart
- iOS: `stop()` is actually a pause — the playback position is preserved
- Web: iOS Safari has significant audio issues (see root CLAUDE.md)
