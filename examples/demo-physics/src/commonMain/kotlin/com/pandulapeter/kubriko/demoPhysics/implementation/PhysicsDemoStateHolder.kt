package com.pandulapeter.kubriko.demoPhysics.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.PolygonBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Disposable
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicChain
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticPolygon
import com.pandulapeter.kubriko.demoPhysics.implementation.managers.PhysicsDemoManager
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.json.Json

sealed interface PhysicsDemoStateHolder : Disposable

internal class PhysicsDemoStateHolderImpl : PhysicsDemoStateHolder {
    private val json = Json { ignoreUnknownKeys = true }
    val serializationManager = EditableMetadata.newSerializationManagerInstance(
        EditableMetadata(
            typeId = "staticBox",
            deserializeState = { serializedState -> json.decodeFromString<StaticBox.State>(serializedState) },
            instantiate = { StaticBox.State(body = RectangleBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "staticCircle",
            deserializeState = { serializedState -> json.decodeFromString<StaticCircle.State>(serializedState) },
            instantiate = { StaticCircle.State(body = CircleBody(initialPosition = it, initialRadius = 100.sceneUnit)) },
        ),
        EditableMetadata(
            typeId = "staticPolygon",
            deserializeState = { serializedState -> json.decodeFromString<StaticPolygon.State>(serializedState) },
            instantiate = {
                StaticPolygon.State(
                    body = PolygonBody(
                        initialPosition = it,
                        vertices = (3..10).random().let { sideCount ->
                            (0..sideCount).map { sideIndex ->
                                val angle = AngleRadians.TwoPi / sideCount * (sideIndex + 0.75f)
                                SceneOffset(
                                    x = (30..120).random().sceneUnit * angle.cos,
                                    y = (30..120).random().sceneUnit * angle.sin,
                                )
                            }
                        },
                    )
                )
            },
        ),
        EditableMetadata(
            typeId = "dynamicBox",
            deserializeState = { serializedState -> json.decodeFromString<DynamicBox.State>(serializedState) },
            instantiate = { DynamicBox.State(body = RectangleBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "dynamicChain",
            deserializeState = { serializedState -> json.decodeFromString<DynamicChain.State>(serializedState) },
            instantiate = { DynamicChain.State(linkCount = 20, initialCenterOffset = it) },
        ),
        EditableMetadata(
            typeId = "dynamicCircle",
            deserializeState = { serializedState -> json.decodeFromString<DynamicCircle.State>(serializedState) },
            instantiate = { DynamicCircle.State(body = CircleBody(initialPosition = it, initialRadius = 20.sceneUnit)) },
        ),
    )

    // The properties below are lazily initialized because we don't need them when we only run the Scene Editor
    private val viewportManager by lazy {
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(
                height = 1920.sceneUnit
            ),
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val physicsManager by lazy {
        PhysicsManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val pointerInputManager by lazy {
        PointerInputManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val physicsDemoManager by lazy {
        PhysicsDemoManager(sceneJson = sceneJson)
    }
    val kubriko by lazy {
        Kubriko.newInstance(
            viewportManager,
            physicsManager,
            pointerInputManager,
            physicsDemoManager,
            serializationManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }

    override fun dispose() = kubriko.dispose()
}

private const val LOG_TAG = "Physics"