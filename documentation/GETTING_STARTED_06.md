# Getting started

These pages guide you through building your first Kubriko game, one small step at a time.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_05.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_07.md)

## 6 - Setting things in motion

A static circle gets boring quickly, so let's make the ball move.

Actors learn new skills by implementing more Traits. To do something on every frame, the `Ball` needs the
**[Dynamic](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Dynamic.kt)** Trait
alongside `Visible`:

```kotlin
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.types.SceneOffset

class Ball : Visible, Dynamic {

    // ...

    private var horizontalSpeed = 0.5f.sceneUnit
    private var verticalSpeed = 0.5f.sceneUnit

    override fun update(deltaTimeInMilliseconds: Int) {
        body.position += SceneOffset(
            x = horizontalSpeed,
            y = verticalSpeed,
        ) * deltaTimeInMilliseconds
    }

    // ...
}
```

`Dynamic` brings in a single function, `update()`, and the engine calls it on every frame while the game is running.

### Why deltaTimeInMilliseconds matters

`deltaTimeInMilliseconds` is how much time passed since the previous frame. Multiplying your movement by it is not optional, it is what keeps your game fair.

One player's device runs at 60 frames per second, another's at 120. Without delta time, the ball would move twice as fast on the second device.
Even on a single device the frame rate fluctuates whenever something heavy happens. Multiplying by delta time cancels all of that out:
the ball covers the same distance per second no matter how many frames it took to get there.

So our speeds are really "scene units per millisecond": at `0.5f`, the ball crosses 500 scene units every second.

Run the game and the ball rolls off diagonally, never to be seen again.

> [!NOTE]
> Kubriko's axes follow the same convention as Compose: X grows to the right, and **Y grows downwards**.
> So a positive `verticalSpeed` moves the ball *down*, which is worth remembering when something moves the wrong way.

> [!TIP]
> If you ever need a fixed time step (a physics simulation, or a deterministic replay), the frame-based loop can be swapped for a fixed-rate or a manual
> `TickSource`. The [TickSource documentation](https://github.com/pandulapeter/kubriko/blob/main/documentation/TICK_SOURCE.md) explains how, and why
> `update()` gets the same delta time either way.

### Staying inside the viewport

To keep the ball on screen, we need to know where the edges of the screen are. That's what `ViewportManager` is for.

We could pass it in through the `Ball` constructor, the same way `GameplayManager` gets `ActorManager`. But Actors have their own way of getting hold of
Managers, and it's worth seeing:

```kotlin
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ViewportManager

class Ball : Visible, Dynamic {

    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }

    // ...
}
```

`onAdded()` comes from the base `Actor` interface, and the engine calls it the moment the Actor joins the scene. It hands you the `Kubriko` instance,
and the `get()` extension pulls any registered Manager out of it. (Its counterpart, `onRemoved()`, is called when the Actor leaves the scene.)

> [!TIP]
> Make sure you import `com.pandulapeter.kubriko.helpers.extensions.get`. It is the reified version, which is what makes `kubriko.get()` work without
> naming the type twice.

Now we can bounce. Replace `update()` with this:

```kotlin
import com.pandulapeter.kubriko.helpers.extensions.constrainedWithin

override fun update(deltaTimeInMilliseconds: Int) {
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
    if (nextPosition.y <= topLeft.y || nextPosition.y >= bottomRight.y) {
        verticalSpeed *= -1
    }
    body.position = nextPosition
}
```

Reading it from the top:

1. `topLeft` and `bottomRight` describe the visible area of the world. We shrink that area by the ball's radius, so the ball bounces when its *edge*
   touches the wall rather than its center. (Remember from page 4: `body.position` is the center of the ball.)
2. We work out where the ball wants to go next, then `constrainedWithin()` pulls that position back inside the allowed area if it overshot.
3. If the constrained position ended up exactly on one of the edges, the ball must have hit that wall, so we flip the matching speed component.
   From the next frame on it travels the other way.

Run the game and the ball bounces around the window. Try it on desktop or web and resize the window while it's running: the walls move with it, and the
ball keeps up without any extra work.

> [!NOTE]
> `topLeft` and `bottomRight` are `StateFlow`s, which is why we read `.value`. The `ViewportManager` reports the viewport's `size` in screen pixels, but
> these two corners are converted to `SceneOffset` for convenience, and they already account for the camera's position and zoom. They always point at the
> world coordinates in the corners of the viewport.

> [!NOTE]
> To save work, the engine stops calling `update()` on `Dynamic` Actors that drift far outside the viewport. Our ball can't leave, so this never affects it.
> If one of your Actors *should* keep running off-screen (a timer, an enemy walking towards the player), override `isAlwaysActive` to return `true`.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_05.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_07.md)
