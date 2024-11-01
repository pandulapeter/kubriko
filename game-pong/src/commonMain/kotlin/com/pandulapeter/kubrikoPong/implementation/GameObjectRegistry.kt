package com.pandulapeter.kubrikoPong.implementation

import com.pandulapeter.kubriko.engine.actor.EditableMetadata
import com.pandulapeter.kubrikoPong.implementation.gameObjects.Box
import kotlinx.serialization.json.Json

object GameObjectRegistry {
    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val typesAvailableInEditor = arrayOf(
        EditableMetadata(Box.TYPE_ID) { serializedState -> json.decodeFromString<Box.BoxState>(serializedState) },
    )
}