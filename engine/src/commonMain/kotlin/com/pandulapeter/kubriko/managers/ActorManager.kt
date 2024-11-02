package com.pandulapeter.kubriko.managers

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.types.SceneOffset
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

    // TODO: No need for this
    fun findVisibleActorsWithPivotsAroundPosition(position: SceneOffset, range: Float): List<Actor>
}