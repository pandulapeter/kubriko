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
        if (stateManager.isRunning.value && deltaTimeInMilliseconds < 1000) {
            actorManager.add(
                particleEmitters.value.flatMap { emitter ->
                    val particleCount = when (val mode = emitter.particleEmissionMode) {
                        is ParticleEmitter.Mode.Burst -> mode.emissionsPerBurst.also { emitter.particleEmissionMode = ParticleEmitter.Mode.Inactive }
                        is ParticleEmitter.Mode.Continuous -> (mode.emissionsPerMillisecond * deltaTimeInMilliseconds).roundToInt()
                        ParticleEmitter.Mode.Inactive -> 0
                    }
                    if (particleCount > 0) (0..particleCount).map {
                        emitter.createParticle()
                    } else emptyList()
                }
            )
        }
    }
}