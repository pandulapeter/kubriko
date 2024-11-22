package com.pandulapeter.kubriko.demoPerformance.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.BoxWithCircle
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.Character
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.MovingBox
import com.pandulapeter.kubriko.demoPerformance.implementation.sceneJson
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shader.ShaderManager
import kotlinx.serialization.json.Json

internal class PerformanceDemoKubrikoWrapper {

    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val performanceDemoManager by lazy {
        PerformanceDemoManager(
            sceneJson = sceneJson,
        )
    }
    val serializationManager by lazy {
        EditableMetadata.newSerializationManagerInstance(
            EditableMetadata(
                typeId = "character",
                deserializeState = { serializedState -> json.decodeFromString<Character.State>(serializedState) },
                instantiate = { Character.State(position = it) },
            ),
            EditableMetadata(
                typeId = "boxWithCircle",
                deserializeState = { serializedState -> json.decodeFromString<BoxWithCircle.State>(serializedState) },
                instantiate = { BoxWithCircle.State(position = it) },
            ),
            EditableMetadata(
                typeId = "movingBox",
                deserializeState = { serializedState -> json.decodeFromString<MovingBox.State>(serializedState) },
                instantiate = { MovingBox.State(position = it) }
            ),
        )
    }
    val kubriko by lazy {
        Kubriko.newInstance(
            KeyboardInputManager.newInstance(),
            ShaderManager.newInstance(),
            performanceDemoManager,
            serializationManager,
        )
    }
}