package com.pandulapeter.gameTemplate.engine.managers

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

interface GameObjectManager {

    fun add(gameObject: GameObject)

    fun add(gameObjects: Collection<GameObject>)

    fun remove(gameObject: GameObject)

    fun remove(gameObjects: Collection<GameObject>)

    fun removeAll()

    fun findGameObjectsWithBoundsInPosition(position: Offset): List<Visible>

    fun findGameObjectsWithPivotsAroundPosition(position: Offset, range: Float): List<Visible>
}