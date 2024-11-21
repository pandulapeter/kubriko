package com.pandulapeter.kubrikoShowcase.implementation.performance

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubrikoShowcase.implementation.performance.actors.BoxWithCircle
import com.pandulapeter.kubrikoShowcase.implementation.performance.actors.Character
import com.pandulapeter.kubrikoShowcase.implementation.performance.actors.MovingBox
import kotlinx.serialization.json.Json

internal class PerformanceShowcaseKubrikoWrapper {

    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val performanceShowcaseManager by lazy {
        PerformanceShowcaseManager(
            mapJson = sceneEditorRealtimeContent,
        )
    }
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
            performanceShowcaseManager,
            serializationManager,
        )
    }
}