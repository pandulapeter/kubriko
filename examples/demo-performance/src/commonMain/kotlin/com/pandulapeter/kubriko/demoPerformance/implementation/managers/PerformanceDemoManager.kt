/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.managers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.demoPerformance.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.demoPerformance.implementation.ui.MiniMap
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import com.pandulapeter.kubriko.uiComponents.LoadingOverlay
import com.pandulapeter.kubriko.uiComponents.Panel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kubriko.examples.demo_performance.generated.resources.Res
import kubriko.examples.demo_performance.generated.resources.description
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class PerformanceDemoManager(
    private val sceneJson: MutableStateFlow<String>?,
) : Manager() {
    private val actorManager by manager<ActorManager>()
    private val viewportManager by manager<ViewportManager>()
    private val metadataManager by manager<MetadataManager>()
    private val stateManager by manager<StateManager>()
    private val serializationManager by manager<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val _shouldShowLoadingIndicator = MutableStateFlow(true)
    private val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()
    private val isSceneEditorEnabled = mutableStateOf(true)

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .onEach(stateManager::updateIsRunning)
            .launchIn(scope)
        actorManager.allActors
            .filter { it.isNotEmpty() }
            .onEach {
                delay(100)
                _shouldShowLoadingIndicator.update { false }
            }
            .launchIn(scope)
        sceneJson?.filter { it.isNotBlank() }?.onEach(::processJson)?.launchIn(scope)
        loadMap()
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) = Box {
        LoadingOverlay(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shouldShowLoadingIndicator = shouldShowLoadingIndicator.collectAsState().value,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
        ) {
            InfoPanel(
                stringResource = Res.string.description,
                isVisible = StateHolder.isInfoPanelVisible.value,
            )
            AnimatedVisibility(
                visible = actorManager.allActors.collectAsState().value.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Panel {
                    MiniMap(
                        miniMapSize = 120.dp,
                        dotRadius = 2.dp,
                        gameTime = metadataManager.totalRuntimeInMilliseconds.filter { it % 4 == 0L }.collectAsState(0L).value,
                        visibleActorColor = MaterialTheme.colorScheme.primary,
                        invisibleActorColor = lerp(LocalContentColor.current, MaterialTheme.colorScheme.surface, 0.8f),
                        getViewportTopLeft = { viewportManager.topLeft.value },
                        getViewportBottomRight = { viewportManager.bottomRight.value },
                        getAllVisibleActors = { actorManager.allActors.value.filterIsInstance<Visible>() },
                        getAllVisibleActorsWithinViewport = { actorManager.visibleActorsWithinViewport.value },
                    )
                }
            }
            if (isSceneEditorEnabled.value) {
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Spacer(
                        modifier = Modifier.weight(1f),
                    )
                    PlatformSpecificContent()
                }
            }
        }
    }

    fun disableSceneEditor() {
        isSceneEditorEnabled.value = false
    }

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
        const val SCENE_NAME = "scene_performance_test.json"
    }
}