package com.pandulapeter.kubrikoStressTest.implementation

import com.pandulapeter.kubriko.engine.editorIntegration.EditableActorMetadata
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.BoxWithCircle
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.Character
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.MovingBox
import kotlinx.serialization.json.Json

object GameObjectRegistry {
    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val typesAvailableInEditor = arrayOf(
        EditableActorMetadata(typeId = "character") { serializedState -> json.decodeFromString<Character.CharacterState>(serializedState) },
        EditableActorMetadata(typeId = "boxWithCircle") { serializedState -> json.decodeFromString<BoxWithCircle.BoxWithCircleState>(serializedState) },
        EditableActorMetadata(typeId = "movingBox") { serializedState -> json.decodeFromString<MovingBox.MovingBoxState>(serializedState) },
    )
}