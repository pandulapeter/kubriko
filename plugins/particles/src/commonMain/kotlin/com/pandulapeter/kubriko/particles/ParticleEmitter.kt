package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.actor.Actor

interface ParticleEmitter : Actor {

    var particleEmissionMode: Mode

    fun createParticle(): Particle

    sealed class Mode {

        data class Continuous(val emissionsPerMillisecond: Float) : Mode()

        data class Burst(val emissionsPerBurst: Int) : Mode()

        data object Inactive : Mode()
    }
}