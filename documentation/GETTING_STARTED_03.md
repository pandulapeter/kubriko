# Getting started

These pages guide you through building your first Kubriko game, one small step at a time.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_02.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_04.md)

## 3 - Showing the game world

A Kubriko game is built from two things that always go together:

- A **[Kubriko](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt) instance**, which holds the entire game state.
- The **[KubrikoViewport](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoViewport.kt) Composable**, which draws that state on screen.

Let's replace the shared module's main Composable (`App.kt`) with those two:

```kotlin
import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport

private val kubriko = Kubriko.newInstance()

@Composable
fun App() = KubrikoViewport(
    kubriko = kubriko,
)
```

Run the app. You should see... an empty screen. That is expected, because we haven't put anything into the game world yet. What matters is that it builds and runs.

### What those few lines do

`Kubriko.newInstance()` creates the engine. It is a plain object, not a Composable, and it owns everything about your game: the objects in the scene, the
camera, whether the game is paused.

`KubrikoViewport` is where the game actually appears. It is a normal Composable, so you can place it anywhere in your UI: full screen, inside a `Column`, next
to other Compose content. It takes a `Modifier` like any other Composable.

As soon as the viewport is on screen, the engine starts its **game loop**: by default it advances the game once per rendered frame, and pauses while the
viewport is not visible or not focused. That source of "ticks" is a separate, replaceable piece called the `TickSource`, which is how the same game logic
can also run in automated tests or without a window at all. We won't need to touch it in this tutorial, but the
[TickSource documentation](https://github.com/pandulapeter/kubriko/blob/main/documentation/TICK_SOURCE.md) explains the options.

### Why the instance lives outside the Composable

This is the part that's easy to get wrong, so it's worth a moment.

The `Kubriko` instance holds your entire game state. If it gets recreated, the game restarts. And Composables get recreated a lot: rotating an Android
device, resizing a desktop window, or simply navigating away and back can all tear down and rebuild the UI.

Declaring it as a top-level `val` sidesteps the problem entirely. The instance is created once, the first time something touches it, and it stays for as
long as the app is running. Rotate the device and the ball keeps bouncing exactly where it was.

That's the right default for most games, where the game *is* the app.

> [!TIP]
> `remember { Kubriko.newInstance() }` looks like the natural Compose thing to do, and it does survive recomposition. It does **not** survive an Android
> configuration change, though, so the game would restart every time the device is rotated. If you want the instance scoped to a screen rather than to the
> app, a ViewModel is a better home for it than `remember`.

### Disposing (for later)

`Kubriko` has a `dispose()` function. It stops the game loop, disposes every Manager, and cancels the engine's coroutine scope.

You don't need it here. When the game's lifetime matches the app's lifetime, letting the process end is enough.

It becomes useful when the game is only part of a bigger app: a mini-game inside a screen the player can leave, a level that gets swapped out for a
different `Kubriko` instance, or several instances that come and go. In those cases, tie `dispose()` to whatever owns the instance, for example a
ViewModel's `onCleared()`. Just make sure that owner survives configuration changes, or you are back to the restarting problem.

The Kubriko Showcase app is an example of the "many instances" setup: every game and demo in it has its own `Kubriko`, and the
[examples folder](https://github.com/pandulapeter/kubriko/tree/main/examples) shows how each one keeps its state alive across configuration changes.

> [!TIP]
> Now is a good moment to check that the IDE can show you Kubriko's sources. Ctrl+click (or Cmd+click) on `KubrikoViewport` and you should land in the
> engine's code, complete with the documentation comments. Every public part of Kubriko is documented that way, and reading those comments is often faster
> than searching the web. The [documentation overview](https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md) is the written
> counterpart: a short description of every building block you are about to meet.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_02.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_04.md)
