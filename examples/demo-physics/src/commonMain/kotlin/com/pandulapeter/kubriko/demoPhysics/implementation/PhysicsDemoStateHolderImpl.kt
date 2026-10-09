/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicChain
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticPolygon
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.randomPolygonVertices
import com.pandulapeter.kubriko.demoPhysics.implementation.managers.PhysicsDemoManager
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PhysicsDemoStateHolderImpl(
    isSceneEditorEnabled: Boolean,
    sceneEditorConnection: SceneEditorConnection?,
    isLoggingEnabled: Boolean,
) : PhysicsDemoStateHolder {

    val serializationManager = EditableMetadata.newSerializationManagerInstance(
        EditableMetadata.create<StaticBox, StaticBox.State>(typeId = "StaticBox") {
            StaticBox.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit)))
        },
        EditableMetadata.create<StaticCircle, StaticCircle.State>(typeId = "StaticCircle") {
            StaticCircle.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit)))
        },
        EditableMetadata.create<StaticPolygon, StaticPolygon.State>(typeId = "StaticPolygon") {
            StaticPolygon.State(
                body = BoxBody(
                    initialPosition = it,
                    initialSize = SceneSize(240.sceneUnit, 240.sceneUnit),
                ),
                vertices = randomPolygonVertices(),
            )
        },
        EditableMetadata.create<DynamicBox, DynamicBox.State>(typeId = "DynamicBox") {
            DynamicBox.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit)))
        },
        EditableMetadata.create<DynamicChain, DynamicChain.State>(typeId = "DynamicChain") {
            DynamicChain.State(linkCount = 20, initialCenterOffset = it)
        },
        EditableMetadata.create<DynamicCircle, DynamicCircle.State>(typeId = "DynamicCircle") {
            DynamicCircle.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit)))
        },
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )

    // The properties below are lazily initialized because we don't need them when we only run the Scene Editor
    private val viewportManager by lazy {
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(
                height = 1920.sceneUnit
            ),
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val physicsManager by lazy {
        PhysicsManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val pointerInputManager by lazy {
        PointerInputManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val physicsDemoManager by lazy {
        PhysicsDemoManager(
            sceneEditorConnection = sceneEditorConnection,
            isSceneEditorEnabled = isSceneEditorEnabled,
        )
    }
    private val _kubriko by lazy {
        MutableStateFlow(
            Kubriko.newInstance(
                viewportManager,
                physicsManager,
                pointerInputManager,
                physicsDemoManager,
                serializationManager,
                isLoggingEnabled = isLoggingEnabled,
                instanceNameForLogging = LOG_TAG,
            )
        )
    }
    override val kubriko by lazy { _kubriko.asStateFlow() }

    override fun dispose() = kubriko.value.dispose()
}

private const val LOG_TAG = "Physics"
