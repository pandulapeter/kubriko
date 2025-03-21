# Getting started

These pages guide you through creating your first Kubriko game from scratch.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_04.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_06.md)

## 5 - Working with Managers

Subclasses of [Manager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/Manager.kt) are
responsible for dealing with the high-level aspects of games such as loading stuff, playing music, keeping score, or manipulating the lower-level Actors.
Managers are there for the entire lifecycle of the `Kubriko` instance, while Actors can be added or removed any time (by
using [ActorManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManager.kt)). Managers
can also communicate directly with each other by having references to other `Manager` instances.

Our really simple game already has four built-in Managers out of the box (more on that later), but to start introducing custom functionality for our gameplay,
we might want to create and add a fifth one.

Let's create a new class called `GameplayManager` that's responsible for adding the `Ball` to the scene:

```kotlin
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager

class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(Ball())
    }
}
```

Our new manager references the built-in `ActorManager`, and notifies it to add a new `Ball` instance when the game starts (the `onInitialize()` function only
gets called once, right before `KubrikoViewport` becomes visible for the first time).

> [!WARNING]  
> Do not try to use `Manager` references before `onInitialize()` is called.

While our `Manager` is now ready, it needs to be registered with the `Kubriko` instance before it can start doing its job. To do this, let's update just one
line in the implementation we have for the `App` Composable:

```kotlin
val kubriko = remember { Kubriko.newInstance(GameplayManager()) }
```

Any number of Managers can be registered simply by enlisting them in the `Kubriko.newInstance()` function.

You can run the game to see how the green ball is now displayed in the middle of the window!

<img src="images/screenshot_my_first_actor.png"  width="250px" />

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_04.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_06.md)