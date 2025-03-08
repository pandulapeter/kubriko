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
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.random.Random

internal class DemoParticleState(
    private var lifespanInMilliseconds: Float,
) : ParticleEmitter.ParticleState() {

    override val body = CircleBody(
        initialRadius = 10.sceneUnit,
    )
    private val speed = 0.4f.sceneUnit
    private var direction = AngleRadians.Zero
    private var hue = 0f
    private var remainingLifespan = 0f
    private var currentProgress = 0f

    init {
        reset(lifespanInMilliseconds)
    }

    fun reset(
        lifespanInMilliseconds: Float,
    ) {
        this.lifespanInMilliseconds = lifespanInMilliseconds
        remainingLifespan = lifespanInMilliseconds
        currentProgress = 0f
        direction = AngleRadians.TwoPi * Random.nextFloat()
        hue = Random.nextFloat() * 360f
        body.position = SceneOffset.Zero
        body.scale = Scale.Unit
    }

    override fun update(deltaTimeInMilliseconds: Int): Boolean {
        currentProgress = 1f - (remainingLifespan / lifespanInMilliseconds)
        if (currentProgress >= 1) {
            return false
        } else {
            body.scale *= (1f - currentProgress / 20f)
            if (body.scale.horizontal < 0.05f) {
                return false
            }
            body.position += SceneOffset(
                x = speed * direction.cos,
                y = - speed * direction.sin,
            ) * deltaTimeInMilliseconds
            remainingLifespan -= deltaTimeInMilliseconds
        }
        return true
    }

    override fun DrawScope.draw() {
        if (currentProgress > 0f) {
            if (currentProgress < 0.7f) {
                drawCircle(
                    color = Color.hsv(
                        hue = (currentProgress * 360f + hue) % 360,
                        saturation = 0.4f,
                        value = 1f,
                    ).copy(alpha = 1f - currentProgress),
                    radius = body.radius.raw,
                    center = body.size.center.raw,
                    style = Fill,
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 1f - currentProgress),
                    radius = body.radius.raw,
                    center = body.size.center.raw,
                    style = Stroke(),
                )
            } else {
                drawCircle(
                    color = Color.Black.copy(alpha = 1f - currentProgress),
                    radius = body.radius.raw,
                    center = body.size.center.raw,
                )
            }
        }
    }
}