/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.AlienShip
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.Ship
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.ShipDestination
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class GameplayManager(
    private val backgroundStateManager: StateManager,
) : Manager() {
    private val actorManager by manager<ActorManager>()
    private val audioManager by manager<AudioManager>()
    private val stateManager by manager<StateManager>()
    private val scoreManager by manager<ScoreManager>()
    private val viewportManager by manager<ViewportManager>()
    private val _isGameOver = MutableStateFlow(true)
    val isGameOver = _isGameOver.asStateFlow()
    val speedMultiplier by lazy { viewportManager.size.map { it.height / 1280f }.asStateFlow(1f) }
    val scaleMultiplier by lazy { viewportManager.size.map { (it.height + it.width) / 3000f }.asStateFlow(1f) }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(
            AlienShip(initialY = 100.sceneUnit),
            AlienShip(initialY = 200.sceneUnit),
            AlienShip(initialY = 300.sceneUnit),
            AlienShip(initialY = 400.sceneUnit),
        )
    }

    fun playGame() {
        if (isGameOver.value) {
            scoreManager.resetScore()
            _isGameOver.update { false }
            actorManager.add(Ship())
            audioManager.playButtonPlaySoundEffect()
        } else {
            audioManager.playButtonToggleSoundEffect()
        }
        actorManager.allActors.value.filterIsInstance<ShipDestination>().firstOrNull()?.resetPointerTracking()
        backgroundStateManager.updateIsRunning(true)
        stateManager.updateIsRunning(true)
    }

    fun pauseGame() {
        if (!isGameOver.value) {
            backgroundStateManager.updateIsRunning(false)
        }
        if (stateManager.isRunning.value) {
            audioManager.playButtonToggleSoundEffect()
        }
        stateManager.updateIsRunning(false)
    }

    fun onGameOver() = _isGameOver.update { true }
}