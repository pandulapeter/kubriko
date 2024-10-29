package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.State
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

interface GameObjectManager {

    val registeredTypeIdsForEditor: StateFlow<List<String>>

    fun getTypeId(type: KClass<out GameObject<*>>): String

    fun register(vararg entries: Triple<String, KClass<out GameObject<*>>, (String) -> State<*>>)

    fun add(vararg gameObjects: GameObject<*>)

    fun remove(vararg gameObjects: GameObject<*>)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    fun findGameObjectsWithBoundsInPosition(position: WorldCoordinates): List<GameObject<*>>

    fun findGameObjectsWithPivotsAroundPosition(position: WorldCoordinates, range: Float): List<GameObject<*>>
}