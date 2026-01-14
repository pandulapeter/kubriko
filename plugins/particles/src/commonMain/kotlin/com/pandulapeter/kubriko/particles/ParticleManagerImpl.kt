/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.particles.implementation.Particle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import kotlin.reflect.KClass

internal class ParticleManagerImpl(
    private val cacheSize: Int,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : ParticleManager(isLoggingEnabled, instanceNameForLogging) {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val particleEmitters by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors
                .filterIsInstance<ParticleEmitter<*>>()
                .toImmutableList()
        }.asStateFlow(persistentListOf())
    }
    private val cache: MutableMap<KClass<out ParticleEmitter.ParticleState>, ArrayDeque<Particle<*>>> = mutableMapOf()

    private fun pop(stateKClass: KClass<out ParticleEmitter.ParticleState>): Particle<*>? = cache[stateKClass]?.removeLastOrNull()

    fun addParticleToCache(stateKClass: KClass<out ParticleEmitter.ParticleState>, particle: Particle<*>) {
        if (cache[stateKClass] == null) {
            cache[stateKClass] = ArrayDeque()
        }
        if ((cache[stateKClass]?.size ?: 0) < cacheSize) {
            cache[stateKClass]?.addLast(particle)
        }
    }

    private var particlesToAdd = mutableListOf<Particle<*>>()

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (stateManager.isRunning.value) {
            particleEmitters.value.forEach { emitter ->
                repeat(
                    when (val mode = emitter.particleEmissionMode) {
                        is ParticleEmitter.Mode.Burst -> mode.emissionsPerBurst.also { emitter.particleEmissionMode = ParticleEmitter.Mode.Inactive }
                        is ParticleEmitter.Mode.Continuous -> (mode.getEmissionsPerMillisecond() * deltaTimeInMilliseconds).roundToInt()
                        ParticleEmitter.Mode.Inactive -> 0
                    }
                ) {
                    particlesToAdd.add(
                        pop(emitter.particleStateType)?.also { reusedParticle ->
                            emitter.reuseParticleInternal(reusedParticle.state)
                        } ?: Particle(emitter.createParticleState())
                    )
                }
            }
            particlesToAdd.let {
                if (it.isNotEmpty()) {
                    actorManager.add(it)
                    particlesToAdd.clear()
                }
            }
        }
    }

    override fun onDispose() = cache.values.forEach { it.clear() }
}