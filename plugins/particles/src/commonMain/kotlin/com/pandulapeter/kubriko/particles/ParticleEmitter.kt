package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.actor.Actor

interface ParticleEmitter<E: ParticleEmitter<E, T>, T : Particle<T>> : Actor {

    var particleEmissionMode: Mode

    fun createParticle(): T

    fun createParticleCache(): Cache<E, T>

    @Suppress("UNCHECKED_CAST")
    class Cache<E: ParticleEmitter<E, T>, T : Particle<T>>(
        private val size: Int = 4000,
        private val reuseParticle: (emitter: E, T) -> Unit,
    ) {
        private val particles: ArrayDeque<T> = ArrayDeque()

        internal fun push(particle: Particle<*>) {
            if (particles.size < size) {
                particles.addLast(particle as T)
            }
        }

        internal fun pop(emitter: ParticleEmitter<*, *>): Particle<T>? = particles.removeLastOrNull()?.also {
            reuseParticle(emitter as E, it)
        }

        fun clear() = particles.clear()
    }

    sealed class Mode {

        data class Continuous(val emissionsPerMillisecond: Float) : Mode()

        data class Burst(val emissionsPerBurst: Int) : Mode()

        data object Inactive : Mode()
    }
}