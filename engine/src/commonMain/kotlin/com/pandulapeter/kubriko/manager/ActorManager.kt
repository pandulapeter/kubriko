package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.manager.ActorManagerImpl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
abstract class ActorManager : Manager() {

    abstract val allActors: StateFlow<ImmutableList<Actor>>
    abstract val visibleActorsWithinViewport: StateFlow<ImmutableList<Visible>>

    abstract fun add(vararg actors: Actor)

    abstract fun add(actors: Collection<Actor>)

    abstract fun remove(vararg actors: Actor)

    abstract fun remove(actors: Collection<Actor>)

    abstract fun removeAll()

    companion object {
        fun newInstance(
            invisibleActorMinimumRefreshTimeInMillis: Long = 100,
        ): ActorManager = ActorManagerImpl(
            invisibleActorMinimumRefreshTimeInMillis = invisibleActorMinimumRefreshTimeInMillis,
        )
    }
}