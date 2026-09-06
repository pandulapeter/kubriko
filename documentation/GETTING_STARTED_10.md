# Getting started

These pages guide you through building your first Kubriko game, one small step at a time.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_09.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_11.md)

## 10 - Sound effects and music

Silence is the last thing standing between us and a finished game. The
[audio-playback](https://github.com/pandulapeter/kubriko/tree/main/plugins/audio-playback) plugin fixes that.

It comes with two Managers, and the split is deliberate:

- **`SoundManager`** plays short sound effects. It keeps them in memory for low latency, and the same sound can play several times at once.
- **`MusicManager`** streams longer background tracks. It can loop them, pause them, and control their volume.

Add the plugin the usual way, to `libs.versions.toml`:

```toml
kubriko-audioPlayback = { group = "io.github.pandulapeter.kubriko", name = "plugin-audio-playback", version.ref = "kubriko" }
```

to the `shared` module's `build.gradle.kts`:

```kotlin
implementation(libs.kubriko.audioPlayback)
```

There is one extra step for this plugin. On Desktop it decodes MP3 files with a library that is hosted on JitPack rather than Maven Central, so that
repository has to be declared too. Add it in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Without it the desktop build fails with `Could not find com.github.umjammer:jlayer`. The other platforms are unaffected, but adding the repository does no harm.

Finally, register the two plugin Managers:

```kotlin
private val kubriko = Kubriko.newInstance(
    CollisionManager.newInstance(),
    KeyboardInputManager.newInstance(),
    PointerInputManager.newInstance(),
    MusicManager.newInstance(),
    SoundManager.newInstance(),
    GameplayManager(),
)
```

### Adding the audio files

Audio files are regular [Compose Multiplatform resources](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources.html).
Drop them into your shared module, under `src/commonMain/composeResources/files/`:

```
composeResources/
└── files/
    ├── music/
    │   └── music.mp3
    └── sounds/
        ├── bounce.wav
        └── game_over.wav
```

The formats are not interchangeable:

- **Sound effects must be WAV.** Keep the bitrate at 48k or below, so older Android devices can handle them.
- **Music should be MP3**, at 320 kbps or less.

If you don't have any audio lying around, sites like [freesound.org](https://freesound.org/) have plenty of files you can use while experimenting.

### An AudioManager

Sound belongs to the game as a whole rather than to any single Actor, so it gets its own Manager. Create `AudioManager.kt`:

```kotlin
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.manager.Manager
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
class AudioManager : Manager() {

    private val musicManager by manager<MusicManager>()
    private val soundManager by manager<SoundManager>()
    private val musicUri = Res.getUri("files/music/music.mp3")
    private val bounceSoundUri = Res.getUri("files/sounds/bounce.wav")
    private val gameOverSoundUri = Res.getUri("files/sounds/game_over.wav")

    override fun onInitialize(kubriko: Kubriko) {
        soundManager.preload(bounceSoundUri, gameOverSoundUri)
        musicManager.play(uri = musicUri, shouldLoop = true)
    }

    fun playBounceSoundEffect() = soundManager.play(bounceSoundUri)

    fun playGameOverSoundEffect() = soundManager.play(gameOverSoundUri)
}
```

`Res` is generated for you by the Compose resources plugin, in a package built from your project and module names. For a project called "BounceGame" with a
`shared` module, the import is `import bouncegame.shared.generated.resources.Res` (with `composeapp` in place of `shared` for the older single-module layout).
The IDE can add it automatically.

`preload()` is the reason the first bounce doesn't arrive late. Without it, a sound is loaded the first time it is played, which can add a
noticeable delay at exactly the wrong moment. Preloading during initialization gets that out of the way.

Register it alongside the others, which brings the list to its final form:

```kotlin
private val kubriko = Kubriko.newInstance(
    CollisionManager.newInstance(),
    KeyboardInputManager.newInstance(),
    PointerInputManager.newInstance(),
    MusicManager.newInstance(),
    SoundManager.newInstance(),
    GameplayManager(),
    AudioManager(),
)
```

### Playing the sounds

`GameplayManager` already knows when interesting things happen, so it is the natural place to trigger them:

```kotlin
class GameplayManager : Manager() {

    private val audioManager by manager<AudioManager>()

    // ...

    fun onPaddleHit() {
        _score.update { it + 1 }
        audioManager.playBounceSoundEffect()
    }

    fun onBallLost() {
        _isGameOver.update { true }
        stateManager.updateIsRunning(false)
        audioManager.playGameOverSoundEffect()
    }
}
```

This is Managers talking to each other, which is what the `manager<T>()` delegate is for. It doesn't matter that `AudioManager` is registered after
`GameplayManager`: the delegate is resolved from the full list of Managers, and these calls happen long after everything is initialized. (The order would
only matter if `GameplayManager` played a sound from inside `onInitialize()`, as page 5 explains.)

Run the game one more time. It has movement, controls, collisions, a score, an ending, and now a soundtrack.

> [!NOTE]
> Audio works on all four platforms, but browsers are strict about it: most of them refuse to start any sound until the player has interacted with the page.
> Music that "doesn't start" on the web is usually this, not a bug in your code. The
> [known issues page](https://github.com/pandulapeter/kubriko/blob/main/documentation/KNOWN_ISSUES.md) lists the rest of the platform quirks.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_09.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_11.md)
