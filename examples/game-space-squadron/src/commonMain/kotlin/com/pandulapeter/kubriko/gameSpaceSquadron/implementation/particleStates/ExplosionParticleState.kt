/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.particleStates

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.random.Random

internal class ExplosionParticleState(
    position: SceneOffset,
    private var color: Color,
) : ParticleEmitter.ParticleState() {

    override val body = CircleBody(
        initialRadius = 4.sceneUnit,
    )
    override val drawingOrder = -1f
    private var speed: SceneUnit = 0.1f.sceneUnit
    private var direction: AngleRadians = AngleRadians.Zero
    private val lifespanInMilliseconds = 400f
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
        body.scale = Scale.Unit
        this.color = color
        direction = AngleRadians.TwoPi * Random.nextFloat()
        remainingLifespan = lifespanInMilliseconds
        currentProgress = 0f
        speed = (0.1f + 0.5f * Random.nextFloat()).sceneUnit
    }

    override fun update(deltaTimeInMilliseconds: Int): Boolean {
        currentProgress = 1f - (remainingLifespan / lifespanInMilliseconds)
        body.scale = Scale.Unit * (1 - currentProgress)
        if (currentProgress >= 1) {
            return false
        } else {
            body.position = SceneOffset(
                x = body.position.x + speed * direction.cos * deltaTimeInMilliseconds,
                y = body.position.y - speed * direction.sin * deltaTimeInMilliseconds,
            )
            remainingLifespan -= deltaTimeInMilliseconds
        }
        return true
    }

    override fun DrawScope.draw() = drawCircle(
        color = color,
        radius = 6f,
        center = body.size.center.raw,
        style = Fill,
    )
}