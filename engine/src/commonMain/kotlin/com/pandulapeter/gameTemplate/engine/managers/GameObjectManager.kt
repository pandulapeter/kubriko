package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.EditorState
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

interface GameObjectManager {

    val registeredTypeIdsForEditor: StateFlow<List<String>>

    fun getTypeId(type: KClass<*>): String

    fun register(vararg entries: Triple<String, KClass<*>, (String) -> EditorState<*>>)

    fun add(vararg gameObjects: Any)

    fun remove(vararg gameObjects: Any)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    fun findGameObjectsWithBoundsInPosition(position: WorldCoordinates): List<Any>

    fun findGameObjectsWithPivotsAroundPosition(position: WorldCoordinates, range: Float): List<Any>
}