package com.pandulapeter.kubriko.engine.managers

import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.StateFlow

interface InstanceManager {

    val allActors: StateFlow<List<Any>>
    val visibleActorsWithinViewport: StateFlow<List<Visible>>

    fun add(vararg actors: Any)

    fun remove(vararg actors: Any)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    // TODO: No need for this
    fun findVisibleInstancesWithBoundsInPosition(position: WorldCoordinates): List<Any>

    // TODO: No need for this
    fun findVisibleInstancesWithPivotsAroundPosition(position: WorldCoordinates, range: Float): List<Any>
}