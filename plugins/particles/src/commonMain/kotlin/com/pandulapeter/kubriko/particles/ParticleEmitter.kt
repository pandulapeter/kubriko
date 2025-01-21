package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.actor.Actor

interface ParticleEmitter<T: Particle> : Actor {

    var particleEmissionMode: Mode

    fun createParticle(): T

    sealed class Mode {

        data class Continuous(val emissionsPerMillisecond: Float) : Mode()

        data class Burst(val emissionsPerBurst: Int) : Mode()

        data object Inactive : Mode()
    }
}