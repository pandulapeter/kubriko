package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

interface InstanceManager {

    // TODO: Should be moved to constructor
    val registeredTypeIdsForEditor: StateFlow<List<String>>
    val allInstances: StateFlow<List<Any>>
    val visibleInstancesWithinViewport: StateFlow<List<Visible>>

    fun resolveTypeId(type: KClass<*>): String

    // TODO: Should be moved to constructor
    fun register(vararg entries: Triple<String, KClass<*>, (String) -> AvailableInEditor.State<*>>)

    fun add(vararg gameObjects: Any)

    fun remove(vararg gameObjects: Any)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    fun findGameObjectsWithBoundsInPosition(position: WorldCoordinates): List<Any>

    fun findGameObjectsWithPivotsAroundPosition(position: WorldCoordinates, range: Float): List<Any>
}