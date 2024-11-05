package com.pandulapeter.kubrikoStressTest.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubrikoStressTest.implementation.actors.BoxWithCircle
import com.pandulapeter.kubrikoStressTest.implementation.actors.Character
import com.pandulapeter.kubrikoStressTest.implementation.actors.MovingBox
import kotlinx.serialization.json.Json

internal class KubrikoWrapper {

    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val gameplayManager by lazy { GameplayManager() }
    val serializationManager by lazy {
        EditableMetadata.newSerializationManagerInstance(
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
    val kubriko by lazy {
        Kubriko.newInstance(
            KeyboardInputManager.newInstance(),
            ShaderManager.newInstance(),
            gameplayManager,
            serializationManager,
        )
    }
}