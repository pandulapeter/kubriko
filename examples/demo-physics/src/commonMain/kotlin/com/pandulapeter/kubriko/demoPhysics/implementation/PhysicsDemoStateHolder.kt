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

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicChain
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticPolygon
import com.pandulapeter.kubriko.demoPhysics.implementation.managers.PhysicsDemoManager
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.chain
import kubriko.examples.demo_physics.generated.resources.description
import kubriko.examples.demo_physics.generated.resources.explosion
import kubriko.examples.demo_physics.generated.resources.ic_chain
import kubriko.examples.demo_physics.generated.resources.ic_explosion
import kubriko.examples.demo_physics.generated.resources.ic_shape
import kubriko.examples.demo_physics.generated.resources.shape

sealed interface PhysicsDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areIconResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_chain).value != null
                && preloadedImageVector(Res.drawable.ic_shape).value != null
                && preloadedImageVector(Res.drawable.ic_explosion).value != null

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
                && preloadedString(Res.string.shape).value.isNotBlank()
                && preloadedString(Res.string.chain).value.isNotBlank()
                && preloadedString(Res.string.explosion).value.isNotBlank()
    }
}

internal class PhysicsDemoStateHolderImpl(
    isSceneEditorEnabled: Boolean,
    isLoggingEnabled: Boolean,
) : PhysicsDemoStateHolder {

    private val json = Json { ignoreUnknownKeys = true }
    val serializationManager = EditableMetadata.newSerializationManagerInstance(
        EditableMetadata(
            typeId = "staticBox",
            deserializeState = { serializedState -> json.decodeFromString<StaticBox.State>(serializedState) },
            instantiate = { StaticBox.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "staticCircle",
            deserializeState = { serializedState -> json.decodeFromString<StaticCircle.State>(serializedState) },
            instantiate = { StaticCircle.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "staticPolygon",
            deserializeState = { serializedState -> json.decodeFromString<StaticPolygon.State>(serializedState) },
            instantiate = {
                StaticPolygon.State(
                    body = BoxBody(
                        initialPosition = it,
                        initialSize = SceneSize(240.sceneUnit, 240.sceneUnit),
                    ),
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
            },
        ),
        EditableMetadata(
            typeId = "dynamicBox",
            deserializeState = { serializedState -> json.decodeFromString<DynamicBox.State>(serializedState) },
            instantiate = { DynamicBox.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "dynamicChain",
            deserializeState = { serializedState -> json.decodeFromString<DynamicChain.State>(serializedState) },
            instantiate = { DynamicChain.State(linkCount = 20, initialCenterOffset = it) },
        ),
        EditableMetadata(
            typeId = "dynamicCircle",
            deserializeState = { serializedState -> json.decodeFromString<DynamicCircle.State>(serializedState) },
            instantiate = { DynamicCircle.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
        ),
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
    val physicsDemoManager by lazy {
        PhysicsDemoManager(
            sceneJson = sceneJson,
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