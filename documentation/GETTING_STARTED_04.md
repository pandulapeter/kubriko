# Getting started

These pages guide you through creating your first Kubriko game from scratch.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_03.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_05.md)

## 4 - Creating an Actor

Implementations of the [Actor](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt) interface
are responsible for playing well-defined roles in your game. For example, in a platformer, the main character (that's controlled by the player) should be an
Actor. But so should every enemy NPC, every platform that the character can jump on, and even the small graphical details such as the trees and clouds in the
background.

Actors are meant to implement one or more interfaces, called Traits, that define what they are capable of (every Trait extends the `Actor` interface). For the
purposes of this demo, we will use
the [Visible](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Visible.kt) Trait which will
allow our Actor to draw onto the scene. Visible Actors need to have bodies that define the position and the size of the area they can draw to (and can also be
used to transform the Actor's drawing canvas by rotation or scaling).

Let's create a brand new class for our first Actor, which will be a green ball.

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

Note that the coordinate system within the game world is measured in SceneUnits. This is important because for more complex games we don't want to confuse world
coordinates with scene coordinates.
While this is not relevant for our current example (as this game's viewport won't support zooming in or out), the API still enforces the distinction.

The default `pivot` point of a body is the center point of its size.

As we didn't specify an `initialPosition` for our `BoxBody`, it will get spawned at the origin of the coordinate system.

> [!IMPORTANT]  
> The origin point of Kubriko's coordinate system is the center of the Composable, NOT any of its corners. This way it's easier to keep the important part of
> the window in focus if it gets resized.

The `draw()` function simply draws a green circle with a black outline. The coordinates here are relative to the `body`, so whenever we want to move our ball,
we just have to update its body's position (no need to change anything in the `draw()` function).

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_03.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_05.md)