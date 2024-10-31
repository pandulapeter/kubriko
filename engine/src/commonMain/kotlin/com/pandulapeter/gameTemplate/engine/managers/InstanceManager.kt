package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.StateFlow

interface InstanceManager {

    val allInstances: StateFlow<List<Any>>
    val visibleInstancesWithinViewport: StateFlow<List<Visible>>

    fun add(vararg instances: Any)

    fun remove(vararg instances: Any)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    fun findVisibleInstancesWithBoundsInPosition(position: WorldCoordinates): List<Any>

    fun findVisibleInstancesWithPivotsAroundPosition(position: WorldCoordinates, range: Float): List<Any>
}