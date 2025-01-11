package com.pandulapeter.kubriko.demoPerformance.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Disposable
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.BoxWithCircle
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.Character
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.MovingBox
import com.pandulapeter.kubriko.demoPerformance.implementation.managers.PerformanceDemoManager
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.json.Json

sealed interface PerformanceDemoStateHolder : Disposable

internal class PerformanceDemoStateHolderImpl : PerformanceDemoStateHolder {
    private val json = Json { ignoreUnknownKeys = true }
    val serializationManager = EditableMetadata.newSerializationManagerInstance(
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
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )

    // The properties below are lazily initialized because we don't need them when we only run the Scene Editor
    private val performanceDemoManager by lazy {
        PerformanceDemoManager(sceneJson = sceneJson)
    }
    private val viewportManager by lazy {
        ViewportManager.newInstance(
            initialScaleFactor = 0.5f,
            viewportEdgeBuffer = 200.sceneUnit,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    val kubriko by lazy {
        Kubriko.newInstance(
            viewportManager,
            performanceDemoManager,
            serializationManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }

    override fun dispose() = kubriko.dispose()
}

private const val LOG_TAG = "Performance"