package com.pandulapeter.gameTemplate.engine.managers

import com.pandulapeter.gameTemplate.engine.gameObject.EditorState

interface SerializationManager {

    suspend fun serializeGameObjectStates(gameObjectStates: List<EditorState<*>>): String

    suspend fun deserializeGameObjectStates(serializedStates: String): List<EditorState<*>>
}