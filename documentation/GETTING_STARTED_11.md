# Getting started

These pages guide you through building your first Kubriko game, one small step at a time.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_10.md)
[<img src="images/badge_next_inactive.png" alt="Next page" height="32px" />](#)

## 11 - Where to go next

Congratulations, you've built a complete little game.

More importantly, you've seen the three ideas that every Kubriko project is made of:

- **Actors** are the things in your game. On their own they do nothing.
- **Traits** are what Actors can do. Add `Visible` to draw, `Dynamic` to think, `Collidable` to bump into things.
- **Managers** are the systems around them: the scene, the camera, the score, the audio.

Everything else, including every plugin, is a variation on those three.

### Things to try next

The fastest way to get comfortable is to keep poking at the game you just made:

- Make the ball speed up a little every time it hits the paddle.
- Shrink the paddle as the score grows.
- Add bricks at the top of the screen and remove them on collision. (`ActorManager.remove()` is all you need.)
- Draw a background, or replace the shapes with sprites using the `sprites` plugin.
- Add a pause button that calls `stateManager.updateIsRunning(false)`.
- Remember the high score between sessions with the `persistence` plugin.

If you want to see where all of those ideas lead, look at
[Wallbreaker](https://github.com/pandulapeter/kubriko/tree/main/examples/game-wallbreaker) in the examples folder. It is essentially this game, grown up:
bricks, menus, a persisted high score, shader effects, and gamepad support, built from the same Actors and Managers you just wrote.

### Other plugins worth a look

The engine core is deliberately small. These plugins extend it, and each one has its own Readme in the
[plugins folder](https://github.com/pandulapeter/kubriko/tree/main/plugins):

| Plugin | What it adds |
|---|---|
| [sprites](https://github.com/pandulapeter/kubriko/tree/main/plugins/sprites) | Images and frame-based animation from sprite sheets. |
| [particles](https://github.com/pandulapeter/kubriko/tree/main/plugins/particles) | Efficient particle effects: explosions, smoke, trails. |
| [physics](https://github.com/pandulapeter/kubriko/tree/main/plugins/physics) | Proper rigid body simulation, with joints and ray casting. |
| [persistence](https://github.com/pandulapeter/kubriko/tree/main/plugins/persistence) | Saving values (like a high score) between sessions. |
| [shaders](https://github.com/pandulapeter/kubriko/tree/main/plugins/shaders) | Visual effects written in SKSL. |
| [gamepad-input](https://github.com/pandulapeter/kubriko/tree/main/plugins/gamepad-input) | Controller support, on all four platforms. |
| [serialization](https://github.com/pandulapeter/kubriko/tree/main/plugins/serialization) | Saving and restoring the whole scene. |

There are also [tools](https://github.com/pandulapeter/kubriko/tree/main/tools) meant for development rather than for players, such as a
**[Debug Menu](https://github.com/pandulapeter/kubriko/tree/main/tools/debug-menu)** with an actor inspector and performance metrics, and a
**[Scene Editor](https://github.com/pandulapeter/kubriko/tree/main/tools/scene-editor)** for building levels visually instead of writing coordinates by hand.

### Going deeper

A few pages are worth reading once the basics feel familiar:

- The [documentation overview](https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md) describes every building block in one place.
- The [list of Traits](https://github.com/pandulapeter/kubriko/blob/main/documentation/LIST_OF_TRAITS.md) and the
  [list of Managers](https://github.com/pandulapeter/kubriko/blob/main/documentation/LIST_OF_MANAGERS.md) are the quickest way to find what the engine and
  its plugins can do. Each entry links to a documented source file.
- The [TickSource documentation](https://github.com/pandulapeter/kubriko/blob/main/documentation/TICK_SOURCE.md) explains the game loop, and how to run
  the engine without a viewport, for tests and simulations.
- The [known issues page](https://github.com/pandulapeter/kubriko/blob/main/documentation/KNOWN_ISSUES.md) lists the platform limitations to plan around.

### Kubriko is a normal Kotlin Multiplatform library

Nothing stops you from using the rest of the ecosystem alongside it:

- **[Ktor](https://ktor.io/)** for networking, leaderboards, or multiplayer.
- **[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)** for save files.
- **[Koin](https://insert-koin.io/)** or any other DI library for wiring bigger projects together.

### Learning from real games

The [examples folder](https://github.com/pandulapeter/kubriko/tree/main/examples) contains everything from tiny feature tests to complete games like
*Annoyed Penguins*, *Wallbreaker*, and *Space Squadron*. They are the same code that runs in the Kubriko Showcase app, and they are the best reference
for how these pieces fit together at a larger scale.

### And if you get stuck

[<img src="images/badge_documentation.png" alt="Documentation" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md)
[<img src="images/badge_community.png" alt="Join the community" height="32px" />](https://discord.gg/RTK4pqbuVR)

The Discord server is there for the questions the documentation doesn't answer.

Happy coding!

### The complete code

For reference, here is every file of the finished game, exactly as the tutorial leaves it. Compare against it if something in your version behaves
differently. (Package declarations and the generated `Res` import are omitted, since they depend on your project's name.)

<details>
<summary><code>App.kt</code></summary>

```kotlin
import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager

private val kubriko = Kubriko.newInstance(
    CollisionManager.newInstance(),
    KeyboardInputManager.newInstance(),
    PointerInputManager.newInstance(),
    MusicManager.newInstance(),
    SoundManager.newInstance(),
    GameplayManager(),
    AudioManager(),
)

@Composable
fun App() = KubrikoViewport(
    kubriko = kubriko,
)
```

</details>

<details>
<summary><code>Ball.kt</code></summary>

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.constrainedWithin
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit

class Ball : Visible, Dynamic, CollisionDetector {

    private val radius = 40.sceneUnit
    override val body = BoxBody(
        initialSize = SceneSize(
            width = radius * 2,
            height = radius * 2,
        ),
    )
    override val collisionMask = CircleCollisionMask(
        initialPosition = body.position,
        initialRadius = radius,
    )
    override val collidableTypes = listOf(Paddle::class)
    private var horizontalSpeed = 0.5f.sceneUnit
    private var verticalSpeed = 0.5f.sceneUnit
    private var isLost = false
    private lateinit var gameplayManager: GameplayManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        gameplayManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isLost) {
            return
        }
        val topLeft = viewportManager.topLeft.value + SceneOffset(radius, radius)
        val bottomRight = viewportManager.bottomRight.value - SceneOffset(radius, radius)
        val speed = SceneOffset(
            x = horizontalSpeed,
            y = verticalSpeed,
        )
        val nextPosition = (body.position + speed * deltaTimeInMilliseconds).constrainedWithin(
            topLeft = topLeft,
            bottomRight = bottomRight,
        )
        if (nextPosition.x <= topLeft.x || nextPosition.x >= bottomRight.x) {
            horizontalSpeed *= -1
        }
        if (nextPosition.y <= topLeft.y) {
            verticalSpeed *= -1
        }
        if (nextPosition.y >= bottomRight.y) {
            isLost = true
            gameplayManager.onBallLost()
        }
        body.position = nextPosition
        collisionMask.position = nextPosition
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        if (!isLost && verticalSpeed > SceneUnit.Zero) {
            verticalSpeed *= -1
            gameplayManager.onPaddleHit()
        }
    }

    override fun DrawScope.draw() {
        drawCircle(
            color = Color.Green,
            radius = radius.raw,
            center = body.pivot.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = radius.raw,
            center = body.pivot.raw,
            style = Stroke(),
        )
    }
}
```

</details>

<details>
<summary><code>Paddle.kt</code></summary>

```kotlin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.constrainedWithin
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.toSceneOffset
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInput.extensions.hasLeft
import com.pandulapeter.kubriko.keyboardInput.extensions.hasRight
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.collections.immutable.ImmutableSet

class Paddle : Visible, Dynamic, Collidable, KeyboardInputAware, PointerInputAware {

    override val body = BoxBody(
        initialSize = SceneSize(
            width = 200.sceneUnit,
            height = 30.sceneUnit,
        ),
    )
    override val collisionMask = BoxCollisionMask(
        initialPosition = body.position,
        initialSize = body.size,
    )
    private var keyboardSpeed = SceneUnit.Zero
    private var pointerX: SceneUnit? = null
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        keyboardSpeed = when {
            activeKeys.hasLeft -> -Speed
            activeKeys.hasRight -> Speed
            else -> SceneUnit.Zero
        }
    }

    override fun onPointerOffsetChanged(pointerId: PointerId?, screenOffset: Offset) {
        pointerX = screenOffset.toSceneOffset(viewportManager).x
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        val halfWidth = body.size.width / 2
        val topLeft = viewportManager.topLeft.value + SceneOffset(halfWidth, SceneUnit.Zero)
        val bottomRight = viewportManager.bottomRight.value - SceneOffset(halfWidth, SceneUnit.Zero)
        val nextX = pointerX ?: (body.position.x + keyboardSpeed * deltaTimeInMilliseconds)
        pointerX = null
        body.position = SceneOffset(
            x = nextX,
            y = bottomRight.y - body.size.height,
        ).constrainedWithin(
            topLeft = topLeft,
            bottomRight = bottomRight,
        )
        collisionMask.position = body.position
    }

    override fun DrawScope.draw() = drawRect(
        color = Color.Blue,
        size = body.size.raw,
    )

    companion object {
        private val Speed = 1f.sceneUnit
    }
}
```

</details>

<details>
<summary><code>GameplayManager.kt</code></summary>

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()
    private val audioManager by manager<AudioManager>()
    private val stateManager by manager<StateManager>()
    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()
    private val _isGameOver = MutableStateFlow(false)
    val isGameOver = _isGameOver.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) = startNewGame()

    fun startNewGame() {
        actorManager.removeAll()
        actorManager.add(Paddle(), Ball())
        _score.update { 0 }
        _isGameOver.update { false }
        stateManager.updateIsRunning(true)
    }

    fun onPaddleHit() {
        _score.update { it + 1 }
        audioManager.playBounceSoundEffect()
    }

    fun onBallLost() {
        _isGameOver.update { true }
        stateManager.updateIsRunning(false)
        audioManager.playGameOverSoundEffect()
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        val currentScore by score.collectAsState()
        val hasGameEnded by isGameOver.collectAsState()
        Box(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets),
        ) {
            Text(
                modifier = Modifier.align(Alignment.TopCenter).padding(all = 16.dp),
                text = "Score: $currentScore",
            )
            if (hasGameEnded) {
                Button(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = ::startNewGame,
                ) {
                    Text(text = "Play again")
                }
            }
        }
    }
}
```

</details>

<details>
<summary><code>AudioManager.kt</code></summary>

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

</details>

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_10.md)
[<img src="images/badge_next_inactive.png" alt="Next page" height="32px" />](#)
