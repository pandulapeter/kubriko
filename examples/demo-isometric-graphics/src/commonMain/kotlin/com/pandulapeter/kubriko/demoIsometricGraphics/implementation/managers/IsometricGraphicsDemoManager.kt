/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class IsometricGraphicsDemoManager(
    private val sceneJson: MutableStateFlow<String>?,
    val isSceneEditorEnabled: Boolean,
    private val actorManager: ActorManager,
    internal val isometricWorldActorManager: ActorManager,
    private val isometricWorldViewportManager: ViewportManager,
    private val gridManager: GridManager,
    private val serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
    private val stateManager: StateManager,
) : Manager(), PointerInputAware, Unique {
    private val _shouldShowLoadingIndicator = MutableStateFlow(true)
    val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()
    val shouldMove = MutableStateFlow(false)
    val shouldRotate = MutableStateFlow(true)
    val shouldBounce = MutableStateFlow(true)
    val shouldDrawDebugBounds = MutableStateFlow(false)
    val characterOrientation = MutableStateFlow(0f)
    private val _areControlsExpanded = MutableStateFlow(false)
    val areControlsExpanded = _areControlsExpanded.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .onEach(stateManager::updateIsRunning)
            .launchIn(scope)
        actorManager.allActors
            .filter { it.isNotEmpty() }
            .onEach {
                delay(300)
                _shouldShowLoadingIndicator.update { false }
            }
            .launchIn(scope)
        sceneJson?.filter { it.isNotBlank() }?.onEach(::processJson)?.launchIn(scope)
        loadMap()
    }

    override fun onPointerDrag(screenOffset: Offset) {
        isometricWorldViewportManager.addToCameraPosition(-screenOffset)
    }

    override fun onPointerZoom(position: Offset, factor: Float) = gridManager.zoom(factor)

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap() = scope.launch {
        try {
            val json = Res.readBytes("files/scenes/$SCENE_NAME").decodeToString()
            sceneJson?.update { json } ?: processJson(json)
        } catch (_: MissingResourceException) {
        }
    }

    private fun processJson(json: String) {
        _shouldShowLoadingIndicator.update { true }
        isometricWorldActorManager.removeAll()
        isometricWorldActorManager.add(this)
        actorManager.removeAll()
        if (json.isNotBlank()) {
            actorManager.add(serializationManager.deserializeActors(json))
        }
        gridManager.tileWidthMultiplier.value = 1f
        gridManager.tileHeightMultiplier.value = 0.5f
        isometricWorldViewportManager.setCameraPosition(SceneOffset.Zero)
    }

    fun toggleControlsExpanded() = _areControlsExpanded.update { !it }

    companion object {
        const val SCENE_NAME = "scene_isometric_graphics_demo.json"
    }
}