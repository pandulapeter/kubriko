/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particleEditor.implementation.manager

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.particles.Particle
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.random.Random

internal class ParticleEditorManager : Manager(), ParticleEmitter, Unique {

    private val actorManager by manager<ActorManager>()
    private val _emissionRate = MutableStateFlow(0.5f)
    val emissionRate = _emissionRate.asStateFlow()
    private val _isEmittingContinuously = MutableStateFlow(true)
    val isEmittingContinuously = _isEmittingContinuously.asStateFlow()
    override val particleEmissionRate get() = emissionRate.value
    override var particleEmissionMode = if (isEmittingContinuously.value) {
        ParticleEmitter.EmissionMode.CONTINUOUS
    } else {
        ParticleEmitter.EmissionMode.BURST_INACTIVE
    }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(this)
        isEmittingContinuously.onEach { isEmittingContinuously ->
            particleEmissionMode = if (isEmittingContinuously) ParticleEmitter.EmissionMode.CONTINUOUS else ParticleEmitter.EmissionMode.BURST_INACTIVE
        }.launchIn(scope)
    }

    fun setEmissionRate(emissionRate: Float) = _emissionRate.update { emissionRate }

    fun onEmittingContinuouslyChanged() = _isEmittingContinuously.update { !it }

    fun burst() {
        particleEmissionMode = ParticleEmitter.EmissionMode.BURST_ACTIVE
    }

    override fun createParticle() = Particle(
        body = RectangleBody(
            initialSize = SceneSize(10.sceneUnit, 10.sceneUnit),
        ),
        speed = 5f.sceneUnit,
        direction = AngleRadians.TwoPi * Random.nextFloat(),
        lifespanInMilliseconds = 1000f,
    )
}