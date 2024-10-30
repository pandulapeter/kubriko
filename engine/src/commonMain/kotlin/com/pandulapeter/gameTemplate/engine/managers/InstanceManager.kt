package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

interface InstanceManager {

    val typeIdsForEditor: Set<String>
    val allInstances: StateFlow<List<Any>>
    val visibleInstancesWithinViewport: StateFlow<List<Visible>>

    fun resolveTypeId(type: KClass<*>): String

    fun add(vararg gameObjects: Any)

    fun remove(vararg gameObjects: Any)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    fun findGameObjectsWithBoundsInPosition(position: WorldCoordinates): List<Any>

    fun findGameObjectsWithPivotsAroundPosition(position: WorldCoordinates, range: Float): List<Any>
}