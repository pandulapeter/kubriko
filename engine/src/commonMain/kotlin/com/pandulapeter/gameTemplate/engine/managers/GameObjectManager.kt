package com.pandulapeter.gameTemplate.engine.managers

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import kotlinx.coroutines.flow.StateFlow

interface GameObjectManager {

    val registeredTypeIds: StateFlow<List<String>>

    fun register(vararg entries: Pair<String, (String) -> GameObject.Serializer<*>>)

    fun add(vararg gameObjects: GameObject<*>)

    fun remove(vararg gameObjects: GameObject<*>)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    fun findGameObjectsWithBoundsInPosition(position: Offset): List<GameObject<*>>

    fun findGameObjectsWithPivotsAroundPosition(position: Offset, range: Float): List<GameObject<*>>
}