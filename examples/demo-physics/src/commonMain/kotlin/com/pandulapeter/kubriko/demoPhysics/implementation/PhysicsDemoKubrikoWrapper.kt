package com.pandulapeter.kubriko.demoPhysics.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticBox
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
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