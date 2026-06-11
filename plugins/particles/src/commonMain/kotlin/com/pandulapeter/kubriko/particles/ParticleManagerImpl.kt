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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

internal class ParticleManagerImpl(
    private val cacheSize: Int,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : ParticleManager(isLoggingEnabled, instanceNameForLogging) {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()

    // Mutable holder keyed by emitter: storing Float values directly in the map would box a fresh
    // Float on every per-frame accumulator write.
    private class EmissionAccumulator {
        var value = 0f
    }

    private val emissionAccumulators = mutableMapOf<ParticleEmitter<*>, EmissionAccumulator>()
    private val particleEmitters by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors
                .filterIsInstance<ParticleEmitter<*>>()
                .toImmutableList()
        }
            .flowOn(Dispatchers.Default)
            .asStateFlow(persistentListOf())
    }
    private val cache: MutableMap<KClass<out ParticleEmitter.ParticleState>, ArrayDeque<Particle<*>>> = mutableMapOf()

    private fun pop(stateKClass: KClass<out ParticleEmitter.ParticleState>): Particle<*>? = cache[stateKClass]?.removeLastOrNull()

    fun addParticleToCache(stateKClass: KClass<out ParticleEmitter.ParticleState>, particle: Particle<*>) {
        val deque = cache.getOrPut(stateKClass) { ArrayDeque() }
        if (deque.size < cacheSize) {
            deque.addLast(particle)
        }
    }

    private val particlesToAdd = mutableListOf<Particle<*>>()

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (stateManager.isRunning.value) {
            val currentEmitters = particleEmitters.value
            currentEmitters.forEach { emitter ->
                val spawnCount = when (val mode = emitter.particleEmissionMode) {
                    is ParticleEmitter.Mode.Burst -> {
                        emitter.particleEmissionMode = ParticleEmitter.Mode.Inactive
                        mode.emissionsPerBurst
                    }

                    is ParticleEmitter.Mode.Continuous -> {
                        val accumulator = emissionAccumulators.getOrPut(emitter) { EmissionAccumulator() }
                        val rawAmount = (mode.getEmissionsPerMillisecond() * deltaTimeInMilliseconds) + accumulator.value
                        val count = rawAmount.toInt()
                        accumulator.value = rawAmount - count
                        count
                    }

                    ParticleEmitter.Mode.Inactive -> 0
                }
                repeat(spawnCount) {
                    particlesToAdd.add(
                        pop(emitter.particleStateType)?.also { reusedParticle ->
                            emitter.reuseParticleInternal(reusedParticle.state)
                        } ?: Particle(emitter.createParticleState())
                    )
                }
            }
            if (emissionAccumulators.size > currentEmitters.size) {
                val iterator = emissionAccumulators.iterator()
                while (iterator.hasNext()) {
                    if (!currentEmitters.contains(iterator.next().key)) {
                        iterator.remove()
                    }
                }
            }
            if (particlesToAdd.isNotEmpty()) {
                actorManager.add(particlesToAdd)
                particlesToAdd.clear()
            }
        }
    }
}