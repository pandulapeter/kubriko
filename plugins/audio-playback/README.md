# Audio Playback Plugin

The `audio-playback` plugin provides high-level managers for playing sound effects and background music in a Kubriko game. It abstracts away platform-specific audio APIs and provides a unified interface for loading and playing audio files.

## Features

- **SoundManager**: Optimized for low-latency playback of short sound effects (SFX). Supports preloading and simultaneous playback of multiple instances of the same sound.
- **MusicManager**: Optimized for streaming larger background music tracks. Supports looping, pausing, and volume control.
- **Platform Support**: Works on Android and Desktop (JVM).

## Usage

### 1. Register the Manager

To use audio in your game, register the `SoundManager` and/or `MusicManager` with your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    SoundManager.newInstance(),
    MusicManager.newInstance(),
    // ... other managers
)
```

### 2. Play Sound Effects

Use the `SoundManager` for short, repetitive sounds like jumps or explosions:

```kotlin
val soundManager = kubriko.get<SoundManager>()

// Play a sound effect
soundManager.play(Res.getUri("sounds/jump.wav"))
```

### 3. Play Background Music

Use the `MusicManager` for larger background music tracks:

```kotlin
val musicManager = kubriko.get<MusicManager>()

// Play background music
musicManager.play(Res.getUri("music/theme.mp3"), shouldLoop = true)
```

## Technical Details

### Supported Formats
- **SFX**: Use WAV files. For Android compatibility, keep the bitrate at a maximum of 48k.
- **Music**: MP3 files at a maximum of 320 kbps are recommended.

### Volume Control
`MusicManager` supports per-track volume control as well as a default volume setting.

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-audio-playback`
