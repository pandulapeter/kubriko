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
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.distanceTo
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.extensions.times
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.particles.Particle
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.random.Random

internal class BulletAlien(
    initialPosition: SceneOffset,
    private val direction: AngleRadians,
) : Visible, Dynamic, ParticleEmitter<BulletAlien, BulletAlien.BulletParticle>, CollisionDetector {
    override val body = CircleBody(
        initialPosition = initialPosition,
        initialRadius = 10f.sceneUnit,
    )
    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: AudioManager
    private lateinit var viewportManager: ViewportManager
    override val drawingOrder = 1f
    override var particleEmissionMode: ParticleEmitter.Mode = ParticleEmitter.Mode.Continuous(
        emissionsPerMillisecond = 0.1f
    )
    override val collidableTypes = listOf(Ship::class)

    override fun createParticleCache() = ParticleEmitter.Cache<BulletAlien, BulletParticle> { emitter, particle ->
        particle.reset(
            initialPosition = emitter.body.position,
            speed = 1f.sceneUnit,
            direction = AngleRadians.TwoPi * Random.nextFloat(),
        )
    }

    override fun DrawScope.draw() = drawCircle(
        radius = body.radius.raw,
        center = body.size.center.raw,
        color = BulletColor,
    )

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        audioManager = kubriko.get()
        viewportManager = kubriko.get()
        kubriko.get<AudioManager>().playShootAlienSoundEffect()
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        body.position = SceneOffset(
            x = body.position.x + direction.cos * Speed,
            y = body.position.y + direction.sin * Speed,
        )
        if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
            actorManager.remove(this)
        }
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        collidables.firstOrNull()?.let { ship ->
            if (body.position.distanceTo(ship.body.position) < CollisionLimit) {
                audioManager.playShipHitSoundEffect()
                actorManager.remove(this)
            }
        }
    }

    override fun createParticle() = BulletParticle(
        emitter = this,
        initialPosition = body.position,
        speed = 1f.sceneUnit,
        direction = AngleRadians.TwoPi * Random.nextFloat(),
    )

    class BulletParticle(
        emitter: BulletAlien,
        initialPosition: SceneOffset,
        speed: SceneUnit,
        direction: AngleRadians,
    ) : Particle<BulletParticle>(
        emitter = emitter,
        speed = speed,
        direction = direction,
    ) {
        private val lifespanInMilliseconds = 300f
        private var remainingLifespan = lifespanInMilliseconds
        private var currentProgress = 0f
        override val drawingOrder = 1f
        override val body = CircleBody(
            initialPosition = initialPosition,
            initialRadius = 4.sceneUnit,
        )

        fun reset(
            initialPosition: SceneOffset,
            speed: SceneUnit,
            direction: AngleRadians,
        ) {
            remainingLifespan = lifespanInMilliseconds
            body.position = initialPosition
            body.scale = Scale.Unit
            this.speed = speed
            this.direction = direction
        }

        override fun updateParticle(deltaTimeInMilliseconds: Float) {
            currentProgress = 1f - (remainingLifespan / lifespanInMilliseconds)
            if (currentProgress >= 1) {
                removeAndCache()
            } else {
                body.scale *= (1f - currentProgress / 5f)
                if (body.scale.horizontal < 0.05f) {
                    removeAndCache()
                }
                remainingLifespan -= deltaTimeInMilliseconds
            }
        }

        override fun DrawScope.draw() = drawCircle(
            color = BulletColor.copy(alpha = 0.8f - currentProgress),
            radius = 6f,
            center = body.size.center.raw,
            style = Fill,
        )
    }

    companion object {
        private val Speed = 6f.sceneUnit
        private val CollisionLimit = 64f.sceneUnit
        private val BulletColor = Color(0xffc29327)
    }
}