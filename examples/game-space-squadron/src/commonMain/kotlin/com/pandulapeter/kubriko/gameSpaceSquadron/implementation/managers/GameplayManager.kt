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
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.Ship
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.ShipDestination
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager

internal class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val audioManager by manager<AudioManager>()

    override fun onInitialize(kubriko: Kubriko) = kubriko.get<ActorManager>().add(Ship())

    fun playGame() {
        audioManager.playButtonPlaySoundEffect()
        actorManager.allActors.value.filterIsInstance<ShipDestination>().firstOrNull()?.resetPointerTracking()
        stateManager.updateIsRunning(true)
    }

    fun pauseGame() {
        audioManager.playButtonToggleSoundEffect()
        stateManager.updateIsRunning(false)
    }
}