# Getting started

These pages guide you through building your first Kubriko game, one small step at a time.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_03.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_05.md)

## 4 - Your first Actor

Everything that exists in a Kubriko game is an
**[Actor](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt)**.
In a platformer, the player character is an Actor. So is every enemy, every platform, and every cloud drifting in the background.

The `Actor` interface itself is almost empty. It only tells the engine "this thing exists". What an Actor can actually *do* comes from **Traits**:
small interfaces you add to it, one capability at a time. There is no base class to extend and no deep hierarchy to learn; you pick the Traits you need
and implement what they ask for.

Our first Actor is a green ball, and for now it only needs one skill: being drawn. That's the
**[Visible](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Visible.kt)** Trait.

Create a new file called `Ball.kt`:

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneSize

class Ball : Visible {

    private val radius = 40.sceneUnit
    override val body = BoxBody(
        initialSize = SceneSize(
            width = radius * 2,
            height = radius * 2,
        ),
    )

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

`Visible` asks for two things, and that's the whole class.

### The body

Every `Visible` Actor needs a `body`. The body answers "where is this thing, and how big is it?". A
[BoxBody](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/body/BoxBody.kt)
is a rectangle with a position, a size, and optionally a rotation and a scale.

Our ball is round, but its body is still a square, because the body is really just the area the Actor is allowed to draw into.
The shape you paint inside it is up to you.

Two details about the body are worth knowing from the start, because they explain a lot of what follows:

- **The pivot.** `body.pivot` is the point the body rotates and scales around. By default it sits in the middle of the body.
- **The position is where the pivot is.** `body.position` does not point at the top left corner of the rectangle. It tells the engine where to put the
  *pivot* in the game world. With the default pivot, that means `position` is simply the center of the Actor, which is usually what you want.

We didn't pass an `initialPosition`, so the ball's center starts at the origin of the game world.

> [!IMPORTANT]
> Where is the origin on screen? Kubriko has a camera, and by default the camera looks at the origin. The camera's position is always the **center** of the
> viewport, so unless you move the camera, the origin is the middle of the screen, not one of its corners. That makes it much easier to keep the
> interesting part of the scene in view when the window gets resized. The camera belongs to the `ViewportManager`, which we'll meet on the next page.

### Scene units

Notice the `40.sceneUnit` instead of a plain `40f`. Kubriko has two separate coordinate systems, and the type system keeps them apart:

- **Scene units** (`SceneUnit`, `SceneOffset`, `SceneSize`) describe positions inside the game world. They are independent of screen resolution and zoom level.
- **Screen pixels** (plain `Float`, `Offset`, `Size`) describe what a specific device is showing right now.

Our small game will never zoom, so the difference doesn't matter much yet. But mixing the two up is a classic source of bugs in bigger games,
which is why the API insists on the distinction from day one. Whenever you need a scene value, the `sceneUnit` extension property gets you one, and `.raw`
gets you back to a plain number. The scene types support the arithmetic you'd expect (`radius * 2` above is one example), and the
[extension helpers](https://github.com/pandulapeter/kubriko/tree/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/extensions) cover the
conversions between the two systems.

The [engine Readme](https://github.com/pandulapeter/kubriko/tree/main/engine#coordinate-system) has a compact description of the coordinate system, and the
[types folder](https://github.com/pandulapeter/kubriko/tree/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/types) holds the definitions.

### Drawing

`draw()` runs inside a Compose [DrawScope](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/drawscope/DrawScope), so everything you
already know about drawing in Compose works here: `drawCircle`, `drawRect`, `drawPath`, `drawImage`, and so on.

The important detail is that the coordinates are **relative to the body**, with `(0, 0)` at its top left corner. The engine has already moved, rotated, and
scaled the canvas for you before calling `draw()`. Since `body.pivot` is the middle of the body, drawing our circle there centers it nicely.

Because of this, moving the ball later will not require touching `draw()` at all. We will just change the body's position, and the drawing follows.

> [!NOTE]
> Anything drawn outside the body's rectangle is clipped away by default. If part of an Actor seems to be missing, the body is probably too small for what
> `draw()` paints. You can turn this off per Actor by overriding `shouldClip`, but a body that matches the drawing is the better fix.

We now have an Actor, but nothing has told the engine about it yet. That's the job of the next step.

> [!TIP]
> `Visible` is one of nine Traits that ship with the engine, and plugins add more. The
> [list of Traits](https://github.com/pandulapeter/kubriko/blob/main/documentation/LIST_OF_TRAITS.md) is the full catalogue; each entry links to a documented
> source file. We'll meet `Dynamic` on page 6 and the collision and input Traits after that.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_03.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_05.md)
