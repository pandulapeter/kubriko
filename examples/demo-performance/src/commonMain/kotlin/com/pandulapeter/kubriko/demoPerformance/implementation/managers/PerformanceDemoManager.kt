/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.managers

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.demoPerformance.implementation.ui.PerformanceDemoOverlay
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kubriko.examples.demo_performance.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class PerformanceDemoManager(
    private val sceneJson: MutableStateFlow<String>?,
    private val isSceneEditorEnabled: Boolean,
) : Manager() {
    private val actorManager by manager<ActorManager>()
    private val viewportManager by manager<ViewportManager>()
    private val metadataManager by manager<MetadataManager>()
    private val serializationManager by manager<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val _shouldShowLoadingIndicator = MutableStateFlow(true)
    private val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
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

    @Composable
    override fun Composable(windowInsets: WindowInsets) = PerformanceDemoOverlay(
        windowInsets = windowInsets,
        shouldShowLoadingIndicator = shouldShowLoadingIndicator.collectAsState().value,
        areActorsLoaded = actorManager.allActors.collectAsState().value.isNotEmpty(),
        totalRuntimeInMilliseconds = metadataManager.totalRuntimeInMilliseconds,
        getViewportTopLeft = { viewportManager.topLeft.value },
        getViewportBottomRight = { viewportManager.bottomRight.value },
        getAllVisibleActors = { actorManager.allActors.value.filterIsInstance<Visible>() },
        getAllVisibleActorsWithinViewport = { actorManager.visibleActorsWithinViewport.value },
        getAllActiveDynamicActors = { actorManager.activeDynamicActors.value.filterIsInstance<Visible>() },
        isSceneEditorEnabled = isSceneEditorEnabled,
    )

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
        actorManager.removeAll()
        val deserializedActors = serializationManager.deserializeActors(json)
        actorManager.add(deserializedActors)
    }

    companion object {
        const val SCENE_NAME = "scene_performance_demo.json"
    }
}