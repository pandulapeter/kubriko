/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.times
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.actors.Blocky
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.shaders.collection.RippleShader
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val backgroundShader = RippleShader()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(
            (-5..5).flatMap { y ->
                (-5..5).map { x ->
                    Blocky(
                        initialPosition = SceneOffset(
                            x = x * 250.sceneUnit,
                            y = y * 250.sceneUnit,
                        )
                    )
                }
            }
        )
        stateManager.isRunning
            .onEach { isRunning ->
                if (isRunning) {
                    actorManager.remove(backgroundShader)
                } else {
                    actorManager.add(backgroundShader)
                }
            }
            .launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (!stateManager.isRunning.value) {
            backgroundShader.update(deltaTimeInMilliseconds)
        }
    }
}