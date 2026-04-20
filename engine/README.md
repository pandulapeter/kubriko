# Engine

This module contains the core components of the Kubriko engine.

It defines the abstract [Manager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/Manager.kt)
and [Actor](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt) contracts, and holds the
implementations for [Kubriko](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt)
and [KubrikoViewport](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoViewport.kt).
Furthermore, it also contains the most important Manager implementations:

- [ActorManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManager.kt),
- [MetadataManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/MetadataManager.kt),
- [StateManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/StateManager.kt), and
- [ViewportManager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ViewportManager.kt).

These four Managers are automatically registered in the `Kubriko` instance, but can be overridden by providing alternative instances from them as arguments of
the `Kubriko.newInstance()` function. This works because the engine ensures that the internal set of Managers is unique by type (and the instances added last
are kept).

Some basic [Actor Traits](https://github.com/pandulapeter/kubriko/tree/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/traits) are also defined
here, together with [custom types](https://github.com/pandulapeter/kubriko/tree/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/types) used in the
public API and some
helpful [extension functions](https://github.com/pandulapeter/kubriko/tree/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/extensions).

Check out the [documentation](https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md) for more details about these components.

## Core Concepts

### Coordinate System
Kubriko distinguishes between two coordinate systems:
- **Scene units** (`SceneUnit`, `SceneOffset`, `SceneSize`): A resolution-independent unit used for game logic and positioning within the world.
- **Screen pixels** (`Float`, `Offset`, `Size`): Standard Compose units used for final rendering and input handling.

- Both coordinate systems are consistent with Compose: X is defined horizontally from left to right while Y determines the vertical measurement from top to bottom.

The `ViewportManager` handles the conversion between these two systems based on the current camera position and zoom level.

### Trait-based Actor System
Instead of a complex inheritance hierarchy, `Actor` behavior is defined by implementing **Traits**. Examples include:
- `Visible`: For actors that should be rendered.
- `Dynamic`: For actors that need to update their state every frame.
- `Positionable`: For actors with a location in the scene.
- `Disposable`: For actors that need to clean up resources when removed.

### Manager Lifecycle
Every `Manager` goes through a defined lifecycle managed by the `Kubriko` instance:
1. `onInitialize()`: Called when the manager is first added.
2. `onUpdate()`: Called every frame (if applicable).
3. `onDispose()`: Called when the engine is shut down.

Managers are added to the engine at the moment of creation and can not be removed later on.
For components that need a more limited lifecycle use Actors. For more complex projects, multiple instances of Kubriko could be used.

## Scope

The `engine` module contains all the essential parts needed in a Kubriko game: the bare minimum feature set for drawing the viewport, dealing with state, and
managing actors. All other features should be added through [plugins](https://github.com/pandulapeter/kubriko/tree/main/plugins) or custom components.

## Public artifact

The artifact for this module has the following ID:
`io.github.pandulapeter.kubriko:engine`