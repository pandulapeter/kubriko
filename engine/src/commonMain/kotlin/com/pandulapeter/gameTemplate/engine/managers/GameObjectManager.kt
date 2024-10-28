package com.pandulapeter.gameTemplate.engine.managers

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectManifest
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible

interface GameObjectManager {

    fun add(gameObject: GameObject)

    fun add(gameObjects: Collection<GameObject>)

    suspend fun addFromJson(json: String, manifest: GameObjectManifest)

    fun remove(gameObject: GameObject)

    fun remove(gameObjects: Collection<GameObject>)

    fun removeAll()

    suspend fun saveToJson(): String

    fun findGameObjectsWithBoundsInPosition(position: Offset): List<Visible>

    fun findGameObjectsWithPivotsAroundPosition(position: Offset, range: Float): List<Visible>
}