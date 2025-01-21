/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoParticles.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.demoParticles.implementation.managers.ParticlesDemoManager
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.particles.Particle
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

internal class DemoParticle(
    emitter: ParticlesDemoManager,
    initialPosition: SceneOffset,
    private var initialHue: Float,
    private var lifespanInMilliseconds: Float,
    speed: SceneUnit = SceneUnit.Zero,
    direction: AngleRadians = AngleRadians.Zero,
) : Particle<DemoParticle>(
    emitter = emitter,
    speed = speed,
    direction = direction,
) {
    private var remainingLifespan = lifespanInMilliseconds
    private var currentProgress = 0f
    override val body = CircleBody(
        initialPosition = initialPosition,
        initialRadius = 10.sceneUnit,
    )

    fun reset(
        initialPosition: SceneOffset,
        initialHue: Float,
        lifespanInMilliseconds: Float,
        speed: SceneUnit,
        direction: AngleRadians,
    ) {
        remainingLifespan = lifespanInMilliseconds
        body.position = initialPosition
        body.scale = Scale.Unit
        body.rotation = AngleRadians.Zero
        this.initialHue = initialHue
        this.lifespanInMilliseconds = lifespanInMilliseconds
        this.speed = speed
        this.direction = direction
    }

    override fun updateParticle(deltaTimeInMilliseconds: Float) {
        currentProgress = 1f - (remainingLifespan / lifespanInMilliseconds)
        if (currentProgress >= 1) {
            removeAndCache()
        } else {
            body.scale *= (1f - currentProgress / 20f)
            if (body.scale.horizontal < 0.05f) {
                removeAndCache()
            }
            remainingLifespan -= deltaTimeInMilliseconds
        }
    }

    override fun DrawScope.draw() = drawCircle(
        color = Color.hsv(
            hue = (currentProgress * 360f + initialHue) % 360,
            saturation = 0.4f,
            value = 1f,
        ).copy(alpha = 1f - currentProgress),
        radius = body.size.raw.maxDimension * 0.7f,
        center = body.size.center.raw,
        style = Fill,
    )
}