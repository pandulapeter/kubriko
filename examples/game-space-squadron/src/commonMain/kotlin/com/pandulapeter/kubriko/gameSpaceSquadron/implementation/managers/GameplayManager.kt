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

import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.Ship
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.ShipDestination
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager

internal class GameplayManager(
    private val backgroundStateManager: StateManager,
) : Manager() {
    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val audioManager by manager<AudioManager>()
    private var isGameOver = true

    fun playGame() {
        if (isGameOver) {
            isGameOver = false
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
        if (!isGameOver) {
            backgroundStateManager.updateIsRunning(false)
        }
        audioManager.playButtonToggleSoundEffect()
        stateManager.updateIsRunning(false)
    }

    fun onGameOver() {
        isGameOver = true
        stateManager.updateIsRunning(false)
    }
}