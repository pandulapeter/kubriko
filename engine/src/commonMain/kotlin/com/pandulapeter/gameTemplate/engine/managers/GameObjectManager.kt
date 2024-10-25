package com.pandulapeter.gameTemplate.engine.managers

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject

interface GameObjectManager {

    fun register(gameObject: GameObject)

    fun register(gameObjects: Collection<GameObject>)

    fun remove(gameObject: GameObject)

    fun remove(gameObjects: Collection<GameObject>)

    fun removeAll()

    fun findGameObjectsOnScreenCoordinates(screenCoordinates: Offset): List<GameObject>
}