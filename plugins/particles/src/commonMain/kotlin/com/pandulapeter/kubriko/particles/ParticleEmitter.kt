package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.actor.Actor

interface ParticleEmitter<T : Particle<T>> : Actor {

    var particleEmissionMode: Mode
    val particleCache: Cache<T>

    fun createParticle(): T

    class Cache<T : Particle<T>>(
        private val reuseParticle: (T) -> Unit,
    ) {
        private val particles: ArrayDeque<T> = ArrayDeque()

        @Suppress("UNCHECKED_CAST")
        internal fun push(particle: Particle<T>) = particles.addLast(particle as T)

        internal fun pop(): Particle<T>? = particles.removeLastOrNull()?.also(reuseParticle)

        fun clear() = particles.clear()
    }

    sealed class Mode {

        data class Continuous(val emissionsPerMillisecond: Float) : Mode()

        data class Burst(val emissionsPerBurst: Int) : Mode()

        data object Inactive : Mode()
    }
}