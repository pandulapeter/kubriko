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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.demoParticles.implementation.ui.EmitterPropertiesPanel
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.particles.Particle
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt
import kotlin.random.Random

internal class ParticlesDemoManager : Manager(), ParticleEmitter, Unique {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val viewportManager by manager<ViewportManager>()
    private val _emissionRate = MutableStateFlow(0.5f)
    val emissionRate = _emissionRate.asStateFlow()
    private val _isEmittingContinuously = MutableStateFlow(true)
    val isEmittingContinuously = _isEmittingContinuously.asStateFlow()
    override var particleEmissionMode = if (isEmittingContinuously.value) {
        ParticleEmitter.Mode.Continuous(emissionRate.value)
    } else {
        ParticleEmitter.Mode.Inactive
    }
    private val _lifespan = MutableStateFlow(500f)
    val lifespan = _lifespan.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(this)
        emissionRate.onEach { emissionRate ->
            if (isEmittingContinuously.value) {
                particleEmissionMode = ParticleEmitter.Mode.Continuous(emissionRate)
            }
        }.launchIn(scope)
        isEmittingContinuously.onEach { isEmittingContinuously ->
            particleEmissionMode = if (isEmittingContinuously) ParticleEmitter.Mode.Continuous(emissionRate.value) else ParticleEmitter.Mode.Inactive
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

    private var counter = 0f

    override fun createParticle() = Particle(
        drawingOrder = counter++,
        payload = Random.nextFloat() * 360f,
        body = RectangleBody(
            initialSize = SceneSize(10.sceneUnit, 10.sceneUnit),
        ),
        speed = 4f.sceneUnit,
        direction = AngleRadians.TwoPi * Random.nextFloat(),
        lifespanInMilliseconds = lifespan.value,
        processBody = { _, progress ->
            scale *= (1f - progress / 10f)
            rotation += AngleRadians.Pi / 20f
        },
        drawParticle = { startingHue, _, progress ->
            drawCircle(
                color = Color.hsv(
                    hue = (progress * 360f + startingHue) % 360,
                    saturation = 0.8f,
                    value = 1f,
                ).copy(alpha = 1f - progress),
                radius = size.maxDimension * 0.7f,
                center = size.center,
                style = Fill,
            )
        }
    )

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        val windowInsets = viewportManager.windowInsets.collectAsState().value.only(WindowInsetsSides.Right)
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            EmitterPropertiesPanel(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(240.dp + windowInsets.asPaddingValues().calculateRightPadding(LocalLayoutDirection.current))
                    .windowInsetsPadding(windowInsets)
                    .fillMaxHeight(),
                particlesDemoManager = this@ParticlesDemoManager,
            )
        }
    }
}