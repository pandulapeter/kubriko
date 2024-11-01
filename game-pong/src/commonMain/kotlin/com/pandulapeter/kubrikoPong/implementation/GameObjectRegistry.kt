package com.pandulapeter.kubrikoPong.implementation

import com.pandulapeter.kubriko.engine.editorIntegration.EditableActorMetadata
import com.pandulapeter.kubrikoPong.implementation.gameObjects.Box
import kotlinx.serialization.json.Json

object GameObjectRegistry {
    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val typesAvailableInEditor = arrayOf(
        EditableActorMetadata(typeId = "box") { serializedState -> json.decodeFromString<Box.BoxState>(serializedState) },
    )
}