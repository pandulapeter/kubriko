/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.shaders.collection.RippleShader
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val loadingManager by manager<LoadingManager>()
    private val backgroundShader = RippleShader()

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isRunning
            .onEach { isRunning ->
                if (isRunning) {
                    actorManager.remove(backgroundShader)
                } else {
                    actorManager.add(backgroundShader)
                }
            }
            .launchIn(scope)
        actorManager.add(loadingManager.actors)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (!stateManager.isRunning.value) {
            backgroundShader.update(deltaTimeInMilliseconds)
        }
    }
}