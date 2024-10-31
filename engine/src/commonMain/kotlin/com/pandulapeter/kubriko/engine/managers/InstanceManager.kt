package com.pandulapeter.kubriko.engine.managers

import com.pandulapeter.kubriko.engine.actor.Actor
import com.pandulapeter.kubriko.engine.actor.traits.Visible
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.StateFlow

interface InstanceManager {

    val allActors: StateFlow<List<Actor>>
    val visibleActorsWithinViewport: StateFlow<List<Visible>>

    fun add(vararg actors: Actor)

    // TODO: Should be based on instanceID
    fun remove(vararg actors: Actor)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    // TODO: No need for this
    fun findVisibleInstancesWithBoundsInPosition(position: WorldCoordinates): List<Actor>

    // TODO: No need for this
    fun findVisibleInstancesWithPivotsAroundPosition(position: WorldCoordinates, range: Float): List<Actor>
}