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
import kotlinx.coroutines.flow.onEach
import kotlin.reflect.KClass

internal class ParticleManagerImpl(
    private val cacheSize: Int,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : ParticleManager(isLoggingEnabled, instanceNameForLogging) {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()

    // Tracks fractional particles to guarantee precise emission rates regardless of framerate
    private val emissionAccumulators = mutableMapOf<ParticleEmitter<*>, Float>()

    private val particleEmitters by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors
                .filterIsInstance<ParticleEmitter<*>>()
                .toImmutableList()
        }.onEach { currentEmitters ->
            // Memory Leak Prevention: Clean up accumulators for emitters that were removed
            emissionAccumulators.keys.retainAll(currentEmitters.toSet())
        }.asStateFlow(persistentListOf())
    }

    private val cache: MutableMap<KClass<out ParticleEmitter.ParticleState>, ArrayDeque<Particle<*>>> = mutableMapOf()

    private fun pop(stateKClass: KClass<out ParticleEmitter.ParticleState>): Particle<*>? = cache[stateKClass]?.removeLastOrNull()

    fun addParticleToCache(stateKClass: KClass<out ParticleEmitter.ParticleState>, particle: Particle<*>) {
        // OPTIMIZATION: 1 Hash Lookup instead of 4
        val deque = cache.getOrPut(stateKClass) { ArrayDeque() }
        if (deque.size < cacheSize) {
            deque.addLast(particle)
        }
    }

    private val particlesToAdd = mutableListOf<Particle<*>>()

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (stateManager.isRunning.value) {
            particleEmitters.value.forEach { emitter ->
                val spawnCount = when (val mode = emitter.particleEmissionMode) {
                    is ParticleEmitter.Mode.Burst -> {
                        emitter.particleEmissionMode = ParticleEmitter.Mode.Inactive
                        mode.emissionsPerBurst
                    }
                    is ParticleEmitter.Mode.Continuous -> {
                        // BUG FIX: The Accumulator Pattern
                        // Calculate total float amount + left over fraction from last frame
                        val rawAmount = (mode.getEmissionsPerMillisecond() * deltaTimeInMilliseconds) + (emissionAccumulators[emitter] ?: 0f)
                        val count = rawAmount.toInt() // Grab the whole numbers to spawn

                        // Save the remaining fraction for the next frame
                        emissionAccumulators[emitter] = rawAmount - count
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

            if (particlesToAdd.isNotEmpty()) {
                actorManager.add(particlesToAdd)
                particlesToAdd.clear()
            }
        }
    }
}