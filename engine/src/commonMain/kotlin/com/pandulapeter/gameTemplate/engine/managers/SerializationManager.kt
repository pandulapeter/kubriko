package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.Serializer

interface SerializationManager {

    suspend fun serializeGameObjectStates(gameObjectSerializers: List<Serializer<*>>): String

    suspend fun deserializeGameObjectStates(serializedStates: String): List<Serializer<*>>
}