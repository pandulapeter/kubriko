package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor

interface SerializationManager {

    suspend fun serializeGameObjectStates(gameObjectStates: List<AvailableInEditor.State<*>>): String

    suspend fun deserializeGameObjectStates(serializedStates: String): List<AvailableInEditor.State<*>>
}