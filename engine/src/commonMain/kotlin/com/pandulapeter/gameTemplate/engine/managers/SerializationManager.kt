package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.State

interface SerializationManager {

    suspend fun serializeGameObjectStates(gameObjectStates: List<State<*>>): String

    suspend fun deserializeGameObjectStates(serializedStates: String): List<State<*>>
}