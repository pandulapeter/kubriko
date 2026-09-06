# Getting started

These pages guide you through building your first Kubriko game, one small step at a time.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_07.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_09.md)

## 8 - Detecting collisions

The ball and the paddle share a screen but not much else. Let's make them notice each other.

Collision detection is another plugin, [collision](https://github.com/pandulapeter/kubriko/tree/main/plugins/collision). Add it the same way as the input
plugins, to `libs.versions.toml`:

```toml
kubriko-collision = { group = "io.github.pandulapeter.kubriko", name = "plugin-collision", version.ref = "kubriko" }
```

to the `shared` module's `build.gradle.kts`:

```kotlin
implementation(libs.kubriko.collision)
```

and to the engine, in `App.kt`:

```kotlin
private val kubriko = Kubriko.newInstance(
    CollisionManager.newInstance(),
    KeyboardInputManager.newInstance(),
    PointerInputManager.newInstance(),
    GameplayManager(),
)
```

### Collision masks

The plugin introduces two Traits:

- **[Collidable](https://github.com/pandulapeter/kubriko/blob/main/plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/Collidable.kt)**
  means "other Actors can bump into me". It needs a `collisionMask`: the shape used for the actual overlap test.
- **[CollisionDetector](https://github.com/pandulapeter/kubriko/blob/main/plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/CollisionDetector.kt)**
  extends `Collidable` with "and I want to be told when that happens".

The mask is separate from the body on purpose. A body is always a rectangle, but a collision shape can be a `PointCollisionMask`, a `CircleCollisionMask`,
a `BoxCollisionMask`, or a `PolygonCollisionMask` (all in the
[mask package](https://github.com/pandulapeter/kubriko/tree/main/plugins/collision/src/commonMain/kotlin/com/pandulapeter/kubriko/collision/mask)).
A round ball deserves a round mask, even though its body is square.

There is one rule to remember: **the mask has its own position, and you have to keep it in sync with the body**. The engine does not do it for you, because
plenty of games want the mask to lag behind or sit somewhere else entirely. In practice this is one line at the end of `update()`.

Like `body.position`, a mask's position is its **center**, so copying the one into the other keeps the two shapes aligned.

### Making the paddle collidable

The paddle only needs to be noticed, so `Collidable` is enough:

```kotlin
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask

class Paddle : Visible, Dynamic, Collidable, KeyboardInputAware, PointerInputAware {

    // ...

    override val collisionMask = BoxCollisionMask(
        initialPosition = body.position,
        initialSize = body.size,
    )

    // ...

    override fun update(deltaTimeInMilliseconds: Int) {
        // ... everything from the previous step ...
        collisionMask.position = body.position
    }
}
```

### Making the ball notice the paddle

The ball is the one that needs to react, so it becomes a `CollisionDetector`:

```kotlin
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.types.SceneUnit

class Ball : Visible, Dynamic, CollisionDetector {

    // ...

    override val collisionMask = CircleCollisionMask(
        initialPosition = body.position,
        initialRadius = radius,
    )
    override val collidableTypes = listOf(Paddle::class)

    override fun onCollisionDetected(collidables: List<Collidable>) {
        if (verticalSpeed > SceneUnit.Zero) {
            verticalSpeed *= -1
        }
    }

    // ...
}
```

`collidableTypes` lists what this detector cares about. Ours only watches for `Paddle`, so the engine can skip everything else. In a bigger game this list
keeps collision checks cheap: an enemy bullet doesn't need to be tested against every decorative rock in the level.

`onCollisionDetected()` receives everything the ball is currently overlapping with. We flip the vertical speed, and the ball bounces upwards.

The `if` matters more than it looks. The `CollisionManager` checks for overlaps on every tick, so collisions are reported on **every frame while the shapes
overlap**, not once per impact. Without the check, a ball that stays in contact for two frames would flip twice and carry on downwards, straight through the
paddle. Testing that the ball is actually moving down before bouncing it up makes the reaction happen exactly once.

Finally, keep the ball's mask in sync too, at the end of its `update()`:

```kotlin
body.position = nextPosition
collisionMask.position = nextPosition
```

### Losing the ball

Right now the ball bounces off the bottom of the screen, which makes the paddle pointless. Let's make the bottom edge dangerous instead.

In the ball's `update()`, split the vertical check in two:

```kotlin
if (nextPosition.y <= topLeft.y) {
    verticalSpeed *= -1
}
if (nextPosition.y >= bottomRight.y) {
    isLost = true
    // tell the rest of the game about it - next page
}
```

The top edge still bounces. The bottom edge is now a loss.

That `isLost` flag is a new property on the ball, and it does more than it looks:

```kotlin
private var isLost = false

override fun update(deltaTimeInMilliseconds: Int) {
    if (isLost) {
        return
    }
    // ... the rest of update() ...
}

override fun onCollisionDetected(collidables: List<Collidable>) {
    if (!isLost && verticalSpeed > SceneUnit.Zero) {
        verticalSpeed *= -1
    }
}
```

Once the ball is lost, it stops doing anything at all.

This matters more than it seems, and it's worth understanding why. **Removing an Actor is not instant.** `ActorManager` batches additions and removals and
applies them on a background thread, so an Actor you removed can still receive one more `update()` before it actually leaves the scene. A ball sitting on the
bottom edge would notice it is past the bottom edge all over again, and report a second loss that never happened.

Without the flag you don't see this at all while the game is over, because nothing is running. It shows up the moment you restart: the first press of a
"Play again" button appears to do nothing except replay the game over sound. That's the old ball, getting one last update before it disappears.

The rule to take away: an Actor keeps running until it is genuinely gone, so anything that should happen exactly once needs to say so.

Run the game and try to keep the ball in the air. It works, but nothing is keeping score yet, and once the ball reaches the bottom it just sits there.

> [!TIP]
> The collision plugin can do more than report overlaps: it can slide a moving Actor along walls instead of letting it pass through, and cast rays for
> line-of-sight checks. The [plugin Readme](https://github.com/pandulapeter/kubriko/tree/main/plugins/collision) covers those. And when you need real
> physics (mass, gravity, bouncing off each other), the [physics plugin](https://github.com/pandulapeter/kubriko/tree/main/plugins/physics) is the
> next step up.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_07.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_09.md)
