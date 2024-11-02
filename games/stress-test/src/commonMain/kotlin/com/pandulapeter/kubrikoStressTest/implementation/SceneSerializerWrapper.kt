package com.pandulapeter.kubrikoStressTest.implementation

import com.pandulapeter.kubriko.sceneSerializer.SceneSerializer
import com.pandulapeter.kubriko.sceneSerializer.integration.EditableMetadata
import com.pandulapeter.kubrikoStressTest.implementation.actors.BoxWithCircle
import com.pandulapeter.kubrikoStressTest.implementation.actors.Character
import com.pandulapeter.kubrikoStressTest.implementation.actors.MovingBox
import kotlinx.serialization.json.Json

class SceneSerializerWrapper {
    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val sceneSerializer = SceneSerializer.newInstance(
        EditableMetadata(typeId = "character") { serializedState -> json.decodeFromString<Character.CharacterState>(serializedState) },
        EditableMetadata(typeId = "boxWithCircle") { serializedState -> json.decodeFromString<BoxWithCircle.BoxWithCircleState>(serializedState) },
        EditableMetadata(typeId = "movingBox") { serializedState -> json.decodeFromString<MovingBox.MovingBoxState>(serializedState) },
    )
}