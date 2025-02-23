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
helpful [extension functions](https://github.com/pandulapeter/kubriko/tree/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/extensions).

Check out the [documentation](https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md) for more details about these components.

## Scope

The `engine` module contains all the essential parts needed in a Kubriko game: the bare minimum feature set for drawing the viewport, dealing with state, and
managing actors. All other features should be added through [plugins](https://github.com/pandulapeter/kubriko/tree/main/plugins) or custom components.

## Public artifact

The artifact for this module has the following ID:
`io.github.pandulapeter.kubriko:engine`