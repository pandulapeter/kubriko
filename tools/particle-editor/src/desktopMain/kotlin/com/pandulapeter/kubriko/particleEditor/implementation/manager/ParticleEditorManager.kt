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
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.particles.Particle
import com.pandulapeter.kubriko.particles.ParticleEmitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class ParticleEditorManager : Manager(), ParticleEmitter, Unique {

    private val actorManager by manager<ActorManager>()
    private val _emissionRate = MutableStateFlow(5f)
    val emissionRate = _emissionRate.asStateFlow()
    private val _isEmittingContinuously = MutableStateFlow(true)
    val isEmittingContinuously = _isEmittingContinuously.asStateFlow()
    override val particleEmissionRate get() = if (isEmittingContinuously.value) emissionRate.value else 0f

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(this)
    }

    fun setEmissionRate(emissionRate: Float) = _emissionRate.update { emissionRate }

    fun onEmittingContinuouslyChanged() = _isEmittingContinuously.update { !it }

    fun burst() {
        // TODO
    }

    override fun createParticle(): Particle {
        TODO("Not yet implemented")
    }
}