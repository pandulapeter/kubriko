# Getting started

These pages guide you through building your first Kubriko game, one small step at a time.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_08.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_10.md)

## 9 - Score, game over, and a bit of UI

We have a game loop. What's missing is everything around it: a score, an ending, and a way to start over.

All of that is global state, which makes it a job for `GameplayManager`.

### Keeping the state

```kotlin
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()
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

    fun onPaddleHit() = _score.update { it + 1 }

    fun onBallLost() {
        _isGameOver.update { true }
        stateManager.updateIsRunning(false)
    }
}
```

Nothing exotic here, just `StateFlow`s holding the score and whether the game has ended, and functions the Actors can call.

`startNewGame()` does double duty: it sets up the first game and restarts after a loss. `removeAll()` clears the scene, then we drop in a fresh paddle and
a fresh ball. Because Actors can be added and removed at any time, "restarting" really is this simple.

### Pausing with StateManager

[StateManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/StateManager.kt) is the
built-in Manager that decides whether the game loop runs. When `isRunning` is false, `Dynamic` Actors stop receiving `update()` calls.
That's exactly what we want on game over: the ball freezes where it is instead of sliding off.

`StateManager` also handles something you get for free: when the window loses focus, or the app goes to the background, the game pauses automatically and
resumes when it comes back. You don't have to write a single line for that. (`isRunning` is only ever true while `isFocused` is true too, and both are
`StateFlow`s you can observe, for example to show a "paused" overlay.)

### Calling back from the Actors

Both Actors already know how to reach a Manager, so this is a small change. In `Ball`:

```kotlin
private lateinit var gameplayManager: GameplayManager

override fun onAdded(kubriko: Kubriko) {
    gameplayManager = kubriko.get()
    viewportManager = kubriko.get()
}
```

Then fill in the two spots we left open on the previous page:

```kotlin
override fun onCollisionDetected(collidables: List<Collidable>) {
    if (!isLost && verticalSpeed > SceneUnit.Zero) {
        verticalSpeed *= -1
        gameplayManager.onPaddleHit()
    }
}
```

```kotlin
if (nextPosition.y >= bottomRight.y) {
    isLost = true
    gameplayManager.onBallLost()
}
```

Setting `isLost` right before the call is what keeps this to a single report, even though the old ball lingers for one more frame after `startNewGame()`
removes it. The previous page explains why.

Notice that `kubriko.get()` works for your own Managers exactly like it does for the built-in ones. As far as the engine is concerned, there is no difference
between them.

### Drawing the UI

The score has to appear somewhere. You have two options, and both are perfectly valid:

1. Put normal Compose UI **around** `KubrikoViewport` in `App.kt`. Good for menus, settings screens, anything that isn't part of the game itself.
   The games in the [examples folder](https://github.com/pandulapeter/kubriko/tree/main/examples) build their menu screens this way.
2. Let a Manager draw **inside** the viewport, by overriding its `Composable()` function.

The second option keeps the score next to the logic that produces it, so let's use that. Add this to `GameplayManager`:

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
```

This is ordinary Compose. The state comes from the same `StateFlow`s the gameplay writes to, so the UI updates itself.

The `windowInsets` parameter is worth using. It describes the areas covered by system bars, notches, and rounded corners, and
`windowInsetsPadding()` keeps your UI clear of them. On a phone this is the difference between a readable score and one hidden behind the status bar.

> [!NOTE]
> Anything you draw here sits **on top** of the game world, and it is not affected by the camera. If you want something that scrolls and zooms with the
> world, that belongs in an Actor's `draw()` instead. There is also a middle ground, the `Overlay` Trait, for Actors that draw in screen space; the
> [list of Traits](https://github.com/pandulapeter/kubriko/blob/main/documentation/LIST_OF_TRAITS.md) has it.

Run the game. Bounce the ball a few times and watch the score climb. Miss it once, and the game freezes with a "Play again" button waiting for you.

That's a complete game loop: play, lose, restart.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_08.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_10.md)
