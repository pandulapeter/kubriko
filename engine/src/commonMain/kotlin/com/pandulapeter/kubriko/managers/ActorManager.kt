package com.pandulapeter.kubriko.managers

import com.pandulapeter.kubriko.actor.Actor
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
interface ActorManager {

    val allActors: StateFlow<List<Actor>>
    val visibleActorsWithinViewport: StateFlow<List<Actor>>

    fun add(vararg actors: Actor)

    fun remove(vararg actors: Actor)

    fun removeAll()
}