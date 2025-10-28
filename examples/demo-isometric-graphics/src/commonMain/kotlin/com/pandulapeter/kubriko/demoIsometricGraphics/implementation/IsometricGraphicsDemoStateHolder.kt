/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.AnimalTile
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.CharacterTile
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.CubeTile
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers.GridManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers.IsometricGraphicsDemoManager
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.description

sealed interface IsometricGraphicsDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areStringResourcesLoaded()

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
    }
}

internal class IsometricGraphicsDemoStateHolderImpl(
    isSceneEditorEnabled: Boolean,
    isLoggingEnabled: Boolean,
) : IsometricGraphicsDemoStateHolder {

    private val json = Json { ignoreUnknownKeys = true }
    val serializationManager = EditableMetadata.newSerializationManagerInstance(
        EditableMetadata(
            typeId = "cube",
            deserializeState = { serializedState -> json.decodeFromString<CubeTile.State>(serializedState) },
            instantiate = {
                CubeTile.State(
                    body = BoxBody(
                        initialPosition = it,
                        initialSize = SceneSize(
                            width = 128.sceneUnit,
                            height = 128.sceneUnit,
                        )
                    )
                )
            },
        ),
        EditableMetadata(
            typeId = "character",
            deserializeState = { serializedState -> json.decodeFromString<CharacterTile.State>(serializedState) },
            instantiate = {
                CharacterTile.State(
                    body = BoxBody(
                        initialPosition = it,
                        initialSize = SceneSize(
                            width = 128.sceneUnit,
                            height = 128.sceneUnit,
                        )
                    )
                )
            },
        ),
        EditableMetadata(
            typeId = "animal",
            deserializeState = { serializedState -> json.decodeFromString<AnimalTile.State>(serializedState) },
            instantiate = {
                AnimalTile.State(
                    body = BoxBody(
                        initialPosition = it,
                        initialSize = SceneSize(
                            width = 128.sceneUnit,
                            height = 128.sceneUnit,
                        )
                    )
                )
            },
        ),
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val gridManager by lazy { GridManager() }
    private val actorManager by lazy {
        ActorManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val stateManager by lazy {
        StateManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    val isometricWorldActorManager = ActorManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_WORLD,
    )
    val isometricWorldViewportManager = ViewportManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_WORLD,
    )
    val spriteManager = SpriteManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_WORLD,
    )
    val isometricGraphicsDemoManager by lazy {
        IsometricGraphicsDemoManager(
            sceneJson = sceneJson,
            isSceneEditorEnabled = isSceneEditorEnabled,
            actorManager = actorManager,
            isometricWorldActorManager = isometricWorldActorManager,
            isometricWorldViewportManager = isometricWorldViewportManager,
            gridManager = gridManager,
            serializationManager = serializationManager,
            stateManager = stateManager,
        )
    }
    private val viewportManager by lazy {
        ViewportManager.newInstance(
            initialScaleFactor = 0.75f,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val pointerInputManager by lazy {
        PointerInputManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG_WORLD,
        )
    }
    private val _kubriko by lazy {
        MutableStateFlow(
            Kubriko.newInstance(
                stateManager,
                viewportManager,
                serializationManager,
                actorManager,
                isometricGraphicsDemoManager,
                isLoggingEnabled = isLoggingEnabled,
                instanceNameForLogging = LOG_TAG,
            )
        )
    }
    override val kubriko by lazy { _kubriko.asStateFlow() }
    internal val isometricWorldKubriko = Kubriko.newInstance(
        gridManager,
        isometricWorldViewportManager,
        isometricWorldActorManager,
        spriteManager,
        pointerInputManager,
        isometricGraphicsDemoManager,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_WORLD,
    )

    override fun dispose() = kubriko.value.dispose()
}

private const val LOG_TAG = "IsometricGraphicsMap"
private const val LOG_TAG_WORLD = "IsometricGraphics"