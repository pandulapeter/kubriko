/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoParticles.implementation.managers

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.demoParticles.implementation.actors.DemoParticleState
import com.pandulapeter.kubriko.demoParticles.implementation.ui.ParticlesDemoOverlay
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.particles.ParticleEmitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

internal class ParticlesDemoManager : Manager(), ParticleEmitter<DemoParticleState>, Unique {

    private val actorManager by manager<ActorManager>()
    private val _emissionRate = MutableStateFlow(0.25f)
    val emissionRate = _emissionRate.asStateFlow()
    private val _isEmittingContinuously = MutableStateFlow(true)
    val isEmittingContinuously = _isEmittingContinuously.asStateFlow()
    override val particleStateType = DemoParticleState::class
    override var particleEmissionMode = if (isEmittingContinuously.value) {
        ParticleEmitter.Mode.Continuous { emissionRate.value }
    } else {
        ParticleEmitter.Mode.Inactive
    }
    private val _lifespan = MutableStateFlow(500f)
    val lifespan = _lifespan.asStateFlow()
    private val _areControlsExpanded = MutableStateFlow(false)
    val areControlsExpanded = _areControlsExpanded.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(this)
        isEmittingContinuously.onEach { isEmittingContinuously ->
            particleEmissionMode = if (isEmittingContinuously) ParticleEmitter.Mode.Continuous { emissionRate.value } else ParticleEmitter.Mode.Inactive
        }.launchIn(scope)
    }

    fun setEmissionRate(emissionRate: Float) = _emissionRate.update { emissionRate }

    fun setLifespan(lifespan: Float) = _lifespan.update { lifespan }

    fun onEmittingContinuouslyChanged() = _isEmittingContinuously.update { !it }

    fun burst() {
        particleEmissionMode = ParticleEmitter.Mode.Burst((emissionRate.value * 100).roundToInt())
    }

    override fun createParticleState() = DemoParticleState(lifespan.value * 6)

    override fun reuseParticleState(state: DemoParticleState) = state.reset(lifespan.value * 6)

    @Composable
    override fun Composable(windowInsets: WindowInsets) = ParticlesDemoOverlay(
        windowInsets = windowInsets,
        areControlsExpanded = areControlsExpanded.collectAsState().value,
        emissionRate = emissionRate.collectAsState().value,
        onEmissionRateChanged = ::setEmissionRate,
        isEmittingContinuously = isEmittingContinuously.collectAsState().value,
        onEmittingContinuouslyChanged = ::onEmittingContinuouslyChanged,
        onBurstButtonPressed = ::burst,
        lifespan = lifespan.collectAsState().value,
        onLifespanChanged = ::setLifespan,
        onControlsToggled = ::toggleControlsExpanded,
    )

    fun toggleControlsExpanded() = _areControlsExpanded.update { !it }
}