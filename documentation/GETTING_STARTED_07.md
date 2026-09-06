# Getting started

These pages guide you through building your first Kubriko game, one small step at a time.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_06.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_08.md)

## 7 - Reacting to player input

So far the game plays itself. Time to give the player something to do: a paddle at the bottom of the screen that they can move left and right.

The engine on its own cannot read the keyboard or the mouse. That kind of feature lives in **plugins**: optional modules you add only when you need them.
This keeps games small, since you never ship code for features you don't use.

We need two of them: [keyboard-input](https://github.com/pandulapeter/kubriko/tree/main/plugins/keyboard-input) and
[pointer-input](https://github.com/pandulapeter/kubriko/tree/main/plugins/pointer-input). ("Pointer" covers the mouse, touch screens, and pen input alike.)

### Adding the plugin dependencies

Back in `gradle/libs.versions.toml`, add two more entries to `[libraries]`. They reuse the same `kubriko` version reference from page 2:

```toml
kubriko-keyboardInput = { group = "io.github.pandulapeter.kubriko", name = "plugin-keyboard-input", version.ref = "kubriko" }
kubriko-pointerInput = { group = "io.github.pandulapeter.kubriko", name = "plugin-pointer-input", version.ref = "kubriko" }
```

And reference them from the `shared` module's `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    //...
    implementation(libs.kubriko.engine)
    implementation(libs.kubriko.keyboardInput)
    implementation(libs.kubriko.pointerInput)
}
```

Every plugin follows this naming pattern, so adding another one later is the same three lines. The full list lives in the
[main Readme](https://github.com/pandulapeter/kubriko/tree/main?tab=readme-ov-file#-artifacts), and each plugin's folder has a Readme describing what it
does; the [plugins overview](https://github.com/pandulapeter/kubriko/tree/main/plugins) links to all of them.

### Registering the plugin Managers

Most plugins work through a Manager, and just like our own `GameplayManager`, they have to be registered:

```kotlin
private val kubriko = Kubriko.newInstance(
    KeyboardInputManager.newInstance(),
    PointerInputManager.newInstance(),
    GameplayManager(),
)
```

> [!IMPORTANT]
> This is the single most common thing to forget. If you add a plugin's Trait to an Actor but not its Manager to the engine, nothing happens.
> No crash, no warning, just silence. If a plugin feature doesn't seem to do anything, check this line first.

Notice that plugin Managers are created with `newInstance()` rather than a constructor. That's the pattern across the whole engine, and it lets Kubriko
pick the right implementation for each platform behind the scenes.

### The Paddle Actor

Create `Paddle.kt`:

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

class Paddle : Visible, Dynamic, KeyboardInputAware, PointerInputAware {

    override val body = BoxBody(
        initialSize = SceneSize(
            width = 200.sceneUnit,
            height = 30.sceneUnit,
        ),
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

There is a lot of import noise, but the class itself is short. Two new Traits are doing the work.

**[KeyboardInputAware](https://github.com/pandulapeter/kubriko/blob/main/plugins/keyboard-input/src/commonMain/kotlin/com/pandulapeter/kubriko/keyboardInput/KeyboardInputAware.kt)**
gives us `handleActiveKeys()`, which is called on every frame with the set of keys currently held down. That is different from a "key pressed" event
(the Trait offers `onKeyPressed()` and `onKeyReleased()` for those): holding a key down keeps the paddle moving, which is exactly what we want. `hasLeft` and
`hasRight` are helper extensions from the plugin, and they cover both the arrow keys and WASD, so you get both control schemes for free.

**[PointerInputAware](https://github.com/pandulapeter/kubriko/blob/main/plugins/pointer-input/src/commonMain/kotlin/com/pandulapeter/kubriko/pointerInput/PointerInputAware.kt)**
gives us `onPointerOffsetChanged()`, which fires whenever a mouse or a finger moves. The position arrives in **screen pixels**, so
`toSceneOffset()` converts it into world coordinates. This is the moment where keeping the two coordinate systems apart pays off.

Both handlers only record what the player asked for. The actual movement happens in `update()`, in one place, so the two input methods can't fight over the
paddle's position. The pointer wins when it moved this frame, otherwise the keyboard speed applies.

The rest of `update()` is the same trick as the ball's: the allowed area is shrunk by half the paddle's width, so the paddle's *edges* stay on screen rather
than its center, and `constrainedWithin()` keeps it there. The Y position is recalculated every frame from `bottomRight.y`, which keeps the paddle glued to
the bottom of the screen even when the window is resized.

> [!TIP]
> Both plugins also expose their Manager for direct queries: `KeyboardInputManager` can tell you whether any key is down right now, and
> `PointerInputManager` publishes the pointer positions as `StateFlow`s. The Traits are usually the simpler choice for Actors, and the Managers come in
> handy for game-wide logic. The plugin Readmes for [keyboard input](https://github.com/pandulapeter/kubriko/tree/main/plugins/keyboard-input) and
> [pointer input](https://github.com/pandulapeter/kubriko/tree/main/plugins/pointer-input) show both styles.

### Putting it in the scene

One line in `GameplayManager`:

```kotlin
override fun onInitialize(kubriko: Kubriko) {
    actorManager.add(Paddle(), Ball())
}
```

`add()` accepts any number of Actors at once, and adding them in one call is a little cheaper than adding them one by one.

Run the game. Move the mouse, or hold the left and right arrow keys, and the paddle follows. The ball still ignores it completely, and it passes straight
through. We'll fix that next.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_06.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_08.md)
