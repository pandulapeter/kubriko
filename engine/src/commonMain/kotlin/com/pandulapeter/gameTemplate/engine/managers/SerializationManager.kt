package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject

interface SerializationManager {

    suspend fun serializeGameObjectStates(gameObjectStates: List<GameObject.State<*>>): String

    suspend fun deserializeGameObjectStates(serializedStates: String): List<GameObject.State<*>>
}