package com.pandulapeter.kubriko.demoPerformance.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.BoxWithCircle
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.Character
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.MovingBox
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.json.Json

internal class PerformanceDemoKubrikoWrapper {

    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val serializationManager by lazy {
        EditableMetadata.newSerializationManagerInstance(
            EditableMetadata(
                typeId = "character",
                deserializeState = { serializedState -> json.decodeFromString<Character.State>(serializedState) },
                instantiate = { Character.State(body = PointBody(initialPosition = it)) },
            ),
            EditableMetadata(
                typeId = "boxWithCircle",
                deserializeState = { serializedState -> json.decodeFromString<BoxWithCircle.State>(serializedState) },
                instantiate = { BoxWithCircle.State(body = RectangleBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
            ),
            EditableMetadata(
                typeId = "movingBox",
                deserializeState = { serializedState -> json.decodeFromString<MovingBox.State>(serializedState) },
                instantiate = { MovingBox.State(body = RectangleBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) }
            ),
        )
    }
    val performanceDemoManager by lazy { PerformanceDemoManager(sceneJson = sceneJson) }
    val kubriko by lazy {
        Kubriko.newInstance(
            ViewportManager.newInstance(
                initialScaleFactor = 0.5f,
                viewportEdgeBuffer = 200.sceneUnit,
            ),
            performanceDemoManager,
            serializationManager,
        )
    }
}