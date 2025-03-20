# Getting started

These pages guide you through creating a your first Kubriko game from scratch.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_05.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_07.md)

## 6 - Setting things in motion

As admiring a static circle might get a bit boring after a while, it's time to make our game more exciting by making the ball move!

As mentioned before, Actors new learn skills by implementing more Trait interfaces. To be able to update its body's state in every frame, the `Ball` Actor
should become [Dynamic](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Dynamic.kt) (
besides already
being [Visible](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Visible.kt)).

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset

class Ball : Visible, Dynamic {
    private val radius = 40.sceneUnit
    override val body = BoxBody(
        initialSize = SceneSize(
            width = radius * 2,
            height = radius * 2,
        ),
    )

    private var horizontalSpeed = 0.5f.sceneUnit
    private var verticalSpeed = 0.5f.sceneUnit

    override fun update(deltaTimeInMilliseconds: Int) {
        body.position += SceneOffset(
            x = horizontalSpeed,
            y = verticalSpeed,
        ) * deltaTimeInMilliseconds
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

Just a couple of additions here: we've implemented the `Dynamic` interface that brought in the `update()` function. Here we move the ball by incrementing its
offset with a value composed by the newly added `horizontalSpeed` and `verticalSpeed` components, multiplied with `deltaTimeInMilliseconds`.

> [!NOTE]
> The `update()` function is invoked by Kubriko's `ActorManager` in every frame, as long as the game is running. The `deltaTimeInMilliseconds` parameter
> provides the number of milliseconds that have passed since drawing the previous frame. It is important to multiply any motion's parameters with this value so
> that we can compensate for changes in the frame rate. Some devices might run our game at 120 FPS, while others only at 90 or 60. Performance-heavy processing
> can introduce fluctuations in the frame rate even on the same device. Using delta time keeps the gameplay balanced by compensating for these fluctuations.

If you run the game now, you should see that the ball moves diagonally out of the viewport.

> [!NOTE]
> Kubriko's coordinate system directions are consistent with Compose: the X axis is incremented from left to right while the Y axis from top to bottom.

Again, once the ball leaves the screen things do get a bit boring, so let's prevent that from happening!

To be able to constrain it within the viewport, we should
get access to the viewport dimensions. There's another built-in manager that's responsible for
this: [ViewportManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ViewportManager.kt). You
should already be able to inject
the `ViewportManager` instance into `Ball` by saving its reference via the `manager<ViewportManager>()` delegate in `GameplayManager` and passing it through the
constructor of `Ball` in the `onInitialize()` function, but let's look at a different approach now:

```kotlin
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ViewportManager

// ...

class Ball : Visible, Dynamic {

    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }

    // ...
}
```

The `onAdded()` function is part of the
base [Actor](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt) interface and is invoked by
`ActorManager` the moment the `Actor` is added to the Scene. As we receive
the `Kubriko` instance as an argument, we can grab references to any registered `Manager` instances from it using the `get` extension function. Make sure you
import the
reified inline function as in the example above.

Now that we can ask the dimensions of the screen from `ViewportManager`, let's modify the `update()` function to make the ball bounce back from the edges of the
viewport:

```kotlin
private var previousPosition = body.position

override fun update(deltaTimeInMilliseconds: Int) {
    val viewportTopLeft = viewportManager.topLeft.value
    val viewportBottomRight = viewportManager.bottomRight.value
    val offset = SceneOffset(
        x = horizontalSpeed,
        y = verticalSpeed,
    )
    val nextPosition = (body.position + offset * deltaTimeInMilliseconds).constrainedWithin(
        topLeft = viewportTopLeft,
        bottomRight = viewportBottomRight,
    )
    var shouldJumpBackToPreviousPosition = false
    if (nextPosition.x == viewportTopLeft.x || nextPosition.x == viewportBottomRight.x) {
        shouldJumpBackToPreviousPosition = true
        horizontalSpeed *= -1
    }
    if (nextPosition.y == viewportTopLeft.y || nextPosition.y == viewportBottomRight.y) {
        shouldJumpBackToPreviousPosition = true
        verticalSpeed *= -1
    }
    if (shouldJumpBackToPreviousPosition) {
        body.position = previousPosition
    }
    previousPosition = body.position
    body.position = nextPosition
}
```

Okay, let's see what's going on here. First we've created an instance variable that holds the position of the Body from the previous frame. This is useful
because whenever the ball is on the edge of the screen, we should move it back to its previous position, to prevent it from getting stuck.

In the `update()` function first we make sure that the next position of the ball is within the viewport bounds by using the `constrainedWithin()` extension
function. But then we test this position against the edges of the screen, and only move the `Body` to it if there was no collision. However, if the ball has
reached one of the edges, we move it back to its previous position as mentioned before, and flip the relevant component of the speed vector so that it will
change its movement direction starting with the next frame.

> [!NOTE]
> While the `ViewportManager` usually deals with screen coordinates, the values coming from the `topLeft` and `bottomRight` Flows are converted to `SceneOffset`
> for convenience. This means that they take into consideration pan and zoom and will always point to the world space coordinates that are marked by the top-left
> and the bottom-right corners of the viewport respectively.

Run the app now to see how the bouncing works! Testing it on desktop or web is especially useful, since you can check how seamlessly the game responds to
changing the window size at runtime.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_05.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_07.md)