# Documentation
This document aims to give a high-level overview of the Kubriko engine's philosophy.

For more detailed information on the various topics check out other Readme files for top-level folders / modules in this repository, and the KDoc comments of the various classes / functions that make up the public API of Kubriko.

## Prerequisites
Using Kubriko requires familiarity with the following concepts:
- [Kotlin](https://kotlinlang.org/)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose](https://developer.android.com/compose)
- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)

## Components of a game
### 💙 Kubriko
The [Kubriko](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt) instance holds references to all the Manager instances for the game, and thus the entire game state.
You can specify these Managers when instantiating Kubriko using `Kubirko.newInstance()`, and persist the returned state holder to seamlessly handle configuration changes (such as a resizing game window).

A Kubriko instance must be provided for KubrikoViewport to work.

### 🖼️ KubrikoViewport
[KubrikoViewport](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoViewport.kt) is a Composable function that displays the game canvas.
It needs a Kubriko instance that controls the game state. It's important to know that Managers only get fully initialized after the first composition of this viewport.

### 🧑‍💼 Managers
Subclasses of [Manager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/Manager.kt) are scoped to the Kubriko instance: they get instantiated together with the engine and during the runtime of the game it's not possible to remove Managers or add new ones.

Use a Manager for any piece of global functionality in your game. There are a number of Managers that come with Kubriko and its plugins, but you can create your own implementations as well.

Managers should be registered using the `Kubriko.newInstance()` function. Even if no managers are specified, the following are instantiated by default, as they are crucial for the engine:
- [ActorManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManager.kt)
- [MetadataManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/MetadataManager.kt)
- [StateManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/StateManager.kt)
- [ViewportManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ViewportManager.kt)

Specifying these explicitly will override the default implementation.

Managers can reference each other and should use their lifecycle functions to control the game state.

The complete list of Managers that come with the engine and its plugins can be found in the following page:

[<img src="images/badge_managers.png" alt="Managers" height="36px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/MANAGERS.md)

### 🎭 Actors
Implementations of [Actor](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt) represent objects or responsibilities within the game. They can be added or removed any time (using Managers) and control in-game objects or perform special tasks.

In a game every character is an Actor. So is every tree, building or vehicle. Actors can form groups, can control gameplay or react to it, etc.

Actors are added / removed using the [ActorManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManager.kt).

What exactly an Actor can or cannot do is determined by Traits: generic interfaces that come with Kubriko and its plugins, or custom ones written for your specific game.

### 🤹 Traits
Traits are interfaces implemented by Actors that extend the Actor's capabilities.

The Actor interface on its own only provides lifecycle callbacks, so Traits are important to make Actors do anything noteworthy, such as draw themselves using the [Visible](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Visible.kt) Trait, or react to the game loop using the [Dynamic](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits/Dynamic.kt) Trait.

The complete list of Traits that come with the engine and its plugins can be found in the following page:

[<img src="images/badge_actor_traits.png" alt="Actor Traits" height="36px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/TRAITS.md)

### ➕ Plugins
The Kubriko engine itself only contains the bare minimum feature set for displaying and managing simple Actors on the viewport.
To extend this functionality in a granular fashion, additional plugin dependencies can be used.

Please note that most plugins expose new Manager classes that need to be registered in the `Kubriko.newInstance()` function to work!

All the plugins provided by Kubriko are located in [this folder](https://github.com/pandulapeter/kubriko/tree/main/plugins).

### 🛠️ Tools
Tools are various solutions provided by Kubriko that don't qualify as plugins, as they are used during development and should not reach players.

One such tool that can be exceptionally useful for more complex projects is the [Scene Editor](https://github.com/pandulapeter/kubriko/tree/main/tools/scene-editor). 

All the tools provided by Kubriko are located in [this folder](https://github.com/pandulapeter/kubriko/tree/main/tools).