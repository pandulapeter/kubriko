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

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import kotlin.reflect.KClass

/**
 * An [Actor] that emits particles.
 *
 * Implementations of this interface define how particles are created, reused, and rendered.
 *
 * @param S The type of the particle state managed by this emitter.
 */
interface ParticleEmitter<S : ParticleEmitter.ParticleState> : Actor {

    /**
     * The current emission mode of the emitter.
     */
    var particleEmissionMode: Mode

    /**
     * The class of the particle state. Used for internal management and pooling.
     */
    val particleStateType: KClass<S>

    /**
     * Creates a new [ParticleState] instance.
     *
     * This is called by the [ParticleManager] when a new particle is needed and the pool is empty.
     */
    fun createParticleState(): S

    /**
     * Re-initializes an existing [ParticleState] instance for reuse.
     *
     * This is called by the [ParticleManager] when a particle is being emitted from the pool.
     * Use this to reset the particle's position, velocity, life, etc.
     */
    fun reuseParticleState(state: S)

    @Suppress("UNCHECKED_CAST")
    fun reuseParticleInternal(state: ParticleState) = reuseParticleState(state as S)

    /**
     * Defines how particles are emitted over time.
     */
    sealed class Mode {

        /**
         * Particles are emitted continuously.
         *
         * @param getEmissionsPerMillisecond A function that returns the number of particles to emit per millisecond.
         */
        data class Continuous(val getEmissionsPerMillisecond: () -> Float) : Mode()

        /**
         * A fixed number of particles are emitted at once.
         *
         * @param emissionsPerBurst The number of particles to emit.
         */
        data class Burst(val emissionsPerBurst: Int) : Mode()

        /**
         * No particles are emitted.
         */
        data object Inactive : Mode()
    }

    /**
     * Represents the state and behavior of a single particle.
     * Acts as a lightweight replacement for a [Visible], [Dynamic] actor.
     */
    abstract class ParticleState {
        /**
         * The bounding box of the particle, used for visibility and clipping.
         */
        abstract val body: BoxBody

        /**
         * The drawing order of the particle.
         */
        open val drawingOrder: Float = 0f

        /**
         * Updates the particle's state.
         *
         * @param deltaTimeInMilliseconds The time elapsed since the last update.
         * @return True if the particle is still alive, false if it should be removed.
         */
        abstract fun update(deltaTimeInMilliseconds: Int): Boolean

        /**
         * Draws the particle.
         */
        abstract fun DrawScope.draw()
    }
}