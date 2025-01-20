/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

internal class ParticleManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : ParticleManager(isLoggingEnabled, instanceNameForLogging) {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val particleEmitters by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors
                .filterIsInstance<ParticleEmitter>()
                .toImmutableList()
        }.asStateFlow(persistentListOf())
    }

    override fun onUpdate(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) {
        if (stateManager.isRunning.value) {
            actorManager.add(
                particleEmitters.value
                    .filter { it.particleEmissionRate > 0 && it.particleEmissionMode == ParticleEmitter.EmissionMode.CONTINUOUS }
                    .flatMap { emitter ->
                        (0..(deltaTimeInMilliseconds * emitter.particleEmissionRate).roundToInt()).map {
                            emitter.createParticle()
                        }
                    } + particleEmitters.value
                    .filter { it.particleEmissionRate > 0 && it.particleEmissionMode == ParticleEmitter.EmissionMode.BURST_ACTIVE }
                    .flatMap { emitter ->
                        (0..(emitter.particleEmissionRate).roundToInt()).map {
                            emitter.createParticle()
                        }.also {
                            emitter.particleEmissionMode = ParticleEmitter.EmissionMode.BURST_INACTIVE
                        }
                    }
            )
        }
    }
}