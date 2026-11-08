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
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.inRenderingMode
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.isBox
import com.pandulapeter.kubriko.demoPerformance.implementation.ui.PerformanceDemoOverlay
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import com.pandulapeter.kubriko.shared.dismissLoadingWhen
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
    private val sceneEditorConnection: SceneEditorConnection?,
    private val isSceneEditorEnabled: Boolean,
) : Manager() {
    private val actorManager by manager<ActorManager>()
    private val viewportManager by manager<ViewportManager>()
    private val metadataManager by manager<MetadataManager>()
    private val serializationManager by manager<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val _shouldShowLoadingIndicator = MutableStateFlow(true)
    private val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()
    private val _isBatched = MutableStateFlow(false)
    private val isBatched = _isBatched.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.allActors.dismissLoadingWhen(
            scope = scope,
            isLoaded = { it.isNotEmpty() },
            onLoaded = { _shouldShowLoadingIndicator.update { false } },
        )
        sceneEditorConnection?.sceneJson?.filter { it.isNotBlank() }?.onEach(::processJson)?.launchIn(scope)
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
        sceneEditorConnection = sceneEditorConnection?.takeIf { isSceneEditorEnabled },
        isBatched = isBatched.collectAsState().value,
        onIsBatchedChanged = ::toggleBatching,
    )

    /** Replaces every box with its counterpart of the other kind, plain or batched, in the same state. */
    private fun toggleBatching() {
        val isBatched = !_isBatched.value
        _isBatched.value = isBatched
        val boxes = actorManager.allActors.value.filter { it.isBox }
        actorManager.remove(boxes)
        actorManager.add(boxes.map { it.inRenderingMode(isBatched) })
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap() = scope.launch {
        try {
            val json = Res.readBytes("files/scenes/$SCENE_NAME").decodeToString()
            sceneEditorConnection?.sceneJson?.update { json } ?: processJson(json)
        } catch (_: MissingResourceException) {
        }
    }

    private fun processJson(json: String) {
        _shouldShowLoadingIndicator.update { true }
        actorManager.removeAll()
        val isBatched = _isBatched.value
        actorManager.add(serializationManager.deserializeActors(json).map { it.inRenderingMode(isBatched) })
    }

    companion object {
        const val SCENE_NAME = "scene_performance_demo.json"
    }
}
