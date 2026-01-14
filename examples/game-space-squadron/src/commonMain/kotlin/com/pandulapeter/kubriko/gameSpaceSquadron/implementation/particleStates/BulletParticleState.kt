/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.random.Random

internal class BulletParticleState(
    position: SceneOffset,
    private var color: Color,
) : ParticleEmitter.ParticleState() {

    override val body = BoxBody(
        initialSize = SceneSize(
            width = Radius * 2,
            height = Radius * 2,
        ),
    )
    override val drawingOrder = 1f
    private val speed: SceneUnit = 0.1f.sceneUnit
    private var direction: AngleRadians = AngleRadians.Zero
    private val lifespanInMilliseconds = 100f
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
            body.scale = Scale.Unit * (1 - currentProgress)
            body.position = SceneOffset(
                x = body.position.x + speed * direction.cos * deltaTimeInMilliseconds,
                y = body.position.y - speed * direction.sin * deltaTimeInMilliseconds,
            )
            remainingLifespan -= deltaTimeInMilliseconds
        }
        return true
    }

    override fun DrawScope.draw() = drawCircle(
        color = color.copy(alpha = 1 - currentProgress),
        radius = Radius.raw,
        center = body.pivot.raw,
        style = Fill,
    )

    companion object {
        private val Radius = 4.sceneUnit
    }
}