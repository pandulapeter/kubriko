/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.GradualBlurShader
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.Star
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.slingshot.Slingshot
import com.pandulapeter.kubriko.helpers.Timer
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val serializationManager by manager<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val viewportManager by manager<ViewportManager>()
    private val _currentLevel = MutableStateFlow<String?>(null)
    val currentLevel = _currentLevel.asStateFlow()
    private val _isLoadingLevel = MutableStateFlow(false)
    val isLoadingLevel = _isLoadingLevel.asStateFlow()
    private val blurShader = GradualBlurShader()
    private val _collectedStarCount = MutableStateFlow(0)
    val collectedStarCount = _collectedStarCount.asStateFlow()
    private val _totalStarCount = MutableStateFlow(0)
    val totalStarCount = _totalStarCount.asStateFlow()
    private var gameEndTimer = Timer(
        timeInMilliseconds = 2000,
        shouldTriggerMultipleTimes = true,
        onDone = { _currentLevel.update { null } }
    )

    override fun onInitialize(kubriko: Kubriko) {
        currentLevel
            .onEach { loadScene(AllLevels[it]) }
            .launchIn(scope)
        stateManager.isRunning
            .onEach { if (it) actorManager.remove(blurShader) else actorManager.add(blurShader) }
            .launchIn(scope)
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadScene(sceneName: String?) = scope.launch {
        _collectedStarCount.update { 0 }
        _totalStarCount.update { 0 }
        if (sceneName == null) {
            stateManager.updateIsRunning(false)
            delay(300)
            actorManager.removeAll()
            stateManager.updateIsRunning(false)
        } else {
            _isLoadingLevel.update { true }
            viewportManager.setScaleFactor(viewportManager.maximumScaleFactor)
            actorManager.removeAll()
            delay(300) // Gives time for the fade animation to hide the previous level
            viewportManager.setCameraPosition(SceneOffset.Zero)
            try {
                val json = Res.readBytes("files/scenes/$sceneName").decodeToString()
                val newActors = serializationManager.deserializeActors(json)
                actorManager.add(newActors)
                _totalStarCount.update { newActors.filterIsInstance<Star>().count() }
                _isLoadingLevel.update { false }
            } catch (_: MissingResourceException) {
            }
        }
    }

    fun onScaleFactorChanged() {
        actorManager.allActors.value.filterIsInstance<Slingshot>().firstOrNull()?.isInitialZoomOutDone = true
    }

    fun onStarCollected() = _collectedStarCount.update { it + 1 }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (!stateManager.isRunning.value) {
            blurShader.update(deltaTimeInMilliseconds)
        }
        if (collectedStarCount.value == totalStarCount.value && totalStarCount.value != 0) {
            gameEndTimer.update(deltaTimeInMilliseconds)
        }
    }

    fun setCurrentLevel(level: String) = _currentLevel.update { level }

    companion object {
        val AllLevels = persistentMapOf(
            "Map 1" to "level_1.json",
            "Map 2" to "level_2.json",
            "Map 3" to "level_3.json",
        )
    }
}