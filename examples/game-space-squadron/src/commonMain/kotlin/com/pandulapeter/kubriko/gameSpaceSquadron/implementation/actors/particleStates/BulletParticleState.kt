/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.particleStates

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.random.Random

class BulletParticleState(
    position: SceneOffset,
    private var color: Color,
) : ParticleEmitter.ParticleState() {

    override val body = CircleBody(
        initialRadius = 4.sceneUnit,
    )
    override val drawingOrder = 1f
    private val speed: SceneUnit = 0.1f.sceneUnit
    private var direction: AngleRadians = AngleRadians.Zero
    private val lifespanInMilliseconds = 300f
    private var remainingLifespan = lifespanInMilliseconds
    private var currentProgress = 0f

    init {
        reset(
            position = position,
            color = color,
        )
    }

    fun reset(
        position: SceneOffset,
        color: Color,
    ) {
        body.position = position
        this.color = color
        body.scale = Scale.Unit
        direction = AngleRadians.TwoPi * Random.nextFloat()
        remainingLifespan = lifespanInMilliseconds
        currentProgress = 0f
    }

    override fun update(deltaTimeInMilliseconds: Int): Boolean {
        currentProgress = 1f - (remainingLifespan / lifespanInMilliseconds)
        if (currentProgress >= 1) {
            return false
        } else {
            body.scale *= (1f - currentProgress / 5f)
            if (body.scale.horizontal < 0.05f) {
                return false
            }
            body.position = SceneOffset(
                x = body.position.x + speed * direction.cos * deltaTimeInMilliseconds,
                y = body.position.y - speed * direction.sin * deltaTimeInMilliseconds,
            )
            remainingLifespan -= deltaTimeInMilliseconds
        }
        return true
    }

    override fun DrawScope.draw() = drawCircle(
        color = color.copy(alpha = 0.8f - currentProgress),
        radius = 6f,
        center = body.size.center.raw,
        style = Fill,
        blendMode = BlendMode.ColorDodge,
    )
}