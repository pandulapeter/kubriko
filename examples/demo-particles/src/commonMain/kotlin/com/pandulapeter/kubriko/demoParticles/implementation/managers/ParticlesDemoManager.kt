/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoParticles.implementation.managers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.demoParticles.implementation.actors.DemoParticleState
import com.pandulapeter.kubriko.demoParticles.implementation.ui.EmitterPropertiesPanel
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.FloatingButton
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.collapse_controls
import kubriko.examples.demo_particles.generated.resources.description
import kubriko.examples.demo_particles.generated.resources.expand_controls
import kubriko.examples.demo_particles.generated.resources.ic_brush
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

internal class ParticlesDemoManager : Manager(), ParticleEmitter<DemoParticleState>, Unique {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
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
        stateManager.isFocused
            .onEach(stateManager::updateIsRunning)
            .launchIn(scope)
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
    override fun Composable(windowInsets: WindowInsets) = Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets)
            .padding(16.dp),
    ) {
        InfoPanel(
            stringResource = Res.string.description,
            isVisible = StateHolder.isInfoPanelVisible.value,
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val areControlsExpanded = areControlsExpanded.collectAsState().value
            this@Column.AnimatedVisibility(
                visible = areControlsExpanded,
                enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 1f)),
                exit = scaleOut(transformOrigin = TransformOrigin(1f, 1f)) + fadeOut(),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    EmitterPropertiesPanel(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .width(240.dp),
                        particlesDemoManager = this@ParticlesDemoManager,
                    )
                }
            }
            FloatingButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                icon = Res.drawable.ic_brush,
                isSelected = areControlsExpanded,
                contentDescription = stringResource(if (areControlsExpanded) Res.string.collapse_controls else Res.string.expand_controls),
                onButtonPressed = ::toggleControlsExpanded,
            )
        }
    }

    fun toggleControlsExpanded() = _areControlsExpanded.update { !it }
}