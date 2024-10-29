package com.pandulapeter.gameTemplate.engine.managers

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.types.MapCoordinates
import kotlinx.coroutines.flow.StateFlow

interface GameObjectManager {

    val registeredTypeIdsForEditor: StateFlow<List<String>>

    fun register(vararg entries: Pair<String, (String) -> Serializer<*>>)

    fun add(vararg gameObjects: GameObject<*>)

    fun remove(vararg gameObjects: GameObject<*>)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    fun findGameObjectsWithBoundsInPosition(position: MapCoordinates): List<GameObject<*>>

    fun findGameObjectsWithPivotsAroundPosition(position: MapCoordinates, range: Float): List<GameObject<*>>
}