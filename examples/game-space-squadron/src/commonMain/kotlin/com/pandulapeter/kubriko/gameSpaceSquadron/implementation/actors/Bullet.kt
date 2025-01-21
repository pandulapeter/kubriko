/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.particles.Particle
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.random.Random

internal class Bullet(
    initialPosition: SceneOffset,
) : Positionable, Dynamic, ParticleEmitter {
    override val body = PointBody(
        initialPosition = initialPosition,
    )
    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager
    override var particleEmissionMode: ParticleEmitter.Mode = ParticleEmitter.Mode.Continuous(
        emissionsPerMillisecond = 0.1f
    )

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        viewportManager = kubriko.get()
        kubriko.get<AudioManager>().playShootSoundEffect()
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        body.position -= SceneOffset.Down * deltaTimeInMilliseconds * 2f
        if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
            actorManager.remove(this)
        }
    }

    override fun createParticle(): Particle = BulletParticle(
        initialPosition = body.position,
    )

    private class BulletParticle(
        initialPosition: SceneOffset,
    ) : Particle(
        speed = 1f.sceneUnit,
        direction = AngleRadians.TwoPi * Random.nextFloat(),
    ) {
        private val lifespanInMilliseconds = 300f
        private var remainingLifespan = lifespanInMilliseconds
        private var currentProgress = 0f
        override val drawingOrder = 1f
        override val body = CircleBody(
            initialPosition = initialPosition,
            initialRadius = 4.sceneUnit,
        )

        override fun updateParticle(deltaTimeInMilliseconds: Float) {
            currentProgress = 1f - (remainingLifespan / lifespanInMilliseconds)
            if (currentProgress >= 1) {
                remove()
            } else {
                body.scale *= (1f - currentProgress / 5f)
                remainingLifespan -= deltaTimeInMilliseconds
            }
        }

        override fun DrawScope.draw() = drawCircle(
            color = Color.White.copy(alpha = 0.8f - currentProgress),
            radius = 6f,
            center = body.size.center.raw,
            style = Fill,
        )
    }
}