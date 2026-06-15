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
import com.pandulapeter.kubriko.particles.implementation.ParticleBatch
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
    private val cache: MutableMap<KClass<out ParticleEmitter.ParticleState>, ArrayDeque<ParticleEmitter.ParticleState>> = mutableMapOf()

    // One rendering actor per distinct drawingOrder, created lazily and kept for the lifetime of the
    // manager. There are very few distinct values in practice, so a linear scan keyed on the Float
    // avoids boxing it as a map key on every emission.
    private val batches = ArrayList<ParticleBatch>()

    // Hoisted so beginFrame() does not allocate a capturing lambda per batch per frame.
    private val recycle: (ParticleEmitter.ParticleState) -> Unit = { state ->
        val deque = cache.getOrPut(state::class) { ArrayDeque() }
        if (deque.size < cacheSize) {
            deque.addLast(state)
        }
    }

    private fun pop(stateKClass: KClass<out ParticleEmitter.ParticleState>): ParticleEmitter.ParticleState? =
        cache[stateKClass]?.removeLastOrNull()

    private fun batchFor(drawingOrder: Float): ParticleBatch {
        for (i in batches.indices) {
            if (batches[i].drawingOrder == drawingOrder) {
                return batches[i]
            }
        }
        return ParticleBatch(drawingOrder).also { batch ->
            batches.add(batch)
            actorManager.add(batch)
        }
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        val isRunning = stateManager.isRunning.value
        for (i in batches.indices) {
            batches[i].beginFrame(deltaTimeInMilliseconds, isRunning, recycle)
        }
        if (isRunning) {
            val currentEmitters = particleEmitters.value
            currentEmitters.forEach { emitter ->
                val mode = emitter.particleEmissionMode
                val spawnCount = when (mode) {
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
                // A continuous emitter conceptually emits steadily across the whole tick, not all at once.
                // Spawning the entire batch on a single instant at the emitter origin renders, at low frame
                // rates, as one expanding shell per tick — i.e. concentric rings. Pre-aging each particle by
                // the slice of this interval it should already have lived scatters the batch along its own
                // trajectory into a continuous stream, frame-rate independently. The effect is proportional
                // to the delta, so it is negligible at 60 FPS and only meaningful once ticks are throttled.
                // Bursts are a single instant by definition, so they are never staggered.
                val shouldStagger = mode is ParticleEmitter.Mode.Continuous && spawnCount > 1 && deltaTimeInMilliseconds > 0
                repeat(spawnCount) { index ->
                    val state = pop(emitter.particleStateType)?.also { reusedState ->
                        emitter.reuseParticleInternal(reusedState)
                    } ?: emitter.createParticleState()
                    if (shouldStagger) {
                        // Oldest first (index 0 ≈ full interval), youngest last (≈ 0), evenly spaced by
                        // delta / spawnCount so successive ticks tile into a uniform age distribution.
                        val preAgeInMilliseconds = (deltaTimeInMilliseconds * (spawnCount - index - 0.5f) / spawnCount).toInt()
                        if (preAgeInMilliseconds > 0 && !state.update(preAgeInMilliseconds)) {
                            // The particle's whole lifetime fell inside the catch-up window; recycle it
                            // instead of adding a particle that would die on its first real frame.
                            recycle(state)
                            return@repeat
                        }
                    }
                    batchFor(state.drawingOrder).addParticle(state)
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
        }
        for (i in batches.indices) {
            batches[i].endFrame()
        }
    }
}
