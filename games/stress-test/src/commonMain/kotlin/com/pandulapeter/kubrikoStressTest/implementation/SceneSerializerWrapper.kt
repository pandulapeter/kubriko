package com.pandulapeter.kubrikoStressTest.implementation

import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.sceneSerializer.SceneSerializer
import com.pandulapeter.kubrikoStressTest.implementation.actors.BoxWithCircle
import com.pandulapeter.kubrikoStressTest.implementation.actors.Character
import com.pandulapeter.kubrikoStressTest.implementation.actors.MovingBox
import kotlinx.serialization.json.Json

class SceneSerializerWrapper {
    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val sceneSerializer = SceneSerializer.newInstance<EditableMetadata<*>, Editable<*>>(
        EditableMetadata(
            typeId = "character",
            deserializeState = { serializedState -> json.decodeFromString<Character.CharacterState>(serializedState) },
            instantiate = { Character.CharacterState(position = it) },
        ),
        EditableMetadata(
            typeId = "boxWithCircle",
            deserializeState = { serializedState -> json.decodeFromString<BoxWithCircle.BoxWithCircleState>(serializedState) },
            instantiate = { BoxWithCircle.BoxWithCircleState(position = it) },
        ),
        EditableMetadata(
            typeId = "movingBox",
            deserializeState = { serializedState -> json.decodeFromString<MovingBox.MovingBoxState>(serializedState) },
            instantiate = { MovingBox.MovingBoxState(position = it) }
        ),
    )
}