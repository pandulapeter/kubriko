package com.pandulapeter.kubriko.demoPhysics.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.PolygonBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticPolygon
import com.pandulapeter.kubriko.implementation.extensions.cos
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.sin
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.json.Json

internal class PhysicsDemoKubrikoWrapper {

    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val physicsDemoManager = PhysicsDemoManager(sceneJson = sceneJson)
    val serializationManager by lazy {
        EditableMetadata.newSerializationManagerInstance(
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
        )
    }
    val kubriko by lazy {
        Kubriko.newInstance(
            ViewportManager.newInstance(aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(defaultHeight = 1920.sceneUnit)),
            PhysicsManager.newInstance(),
            PointerInputManager.newInstance(),
            physicsDemoManager,
            serializationManager,
        )
    }
}