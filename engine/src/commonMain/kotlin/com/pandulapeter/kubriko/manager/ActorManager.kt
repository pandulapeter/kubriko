package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.implementation.manager.ActorManagerImpl
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
abstract class ActorManager : Manager() {

    abstract val allActors: StateFlow<List<Actor>>
    abstract val visibleActorsWithinViewport: StateFlow<List<Actor>>

    abstract fun add(vararg actors: Actor)

    abstract fun remove(vararg actors: Actor)

    abstract fun removeAll()

    companion object {
        fun newInstance(
            invisibleActorMinimumRefreshTimeInMillis: Long = 100,
        ): ActorManager = ActorManagerImpl(
            invisibleActorMinimumRefreshTimeInMillis = invisibleActorMinimumRefreshTimeInMillis,
        )
    }
}