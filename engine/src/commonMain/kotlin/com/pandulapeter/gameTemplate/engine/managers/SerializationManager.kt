package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject

interface SerializationManager {

    suspend fun serializeGameObjectStates(gameObjectSerializers: List<GameObject.Serializer<*>>): String

    suspend fun deserializeGameObjectStates(serializedStates: String): List<GameObject.Serializer<*>>
}