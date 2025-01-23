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
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.particleStates.BulletParticleState
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

internal class BulletAlien(
    initialPosition: SceneOffset,
    private val direction: AngleRadians,
) : Visible, Dynamic, ParticleEmitter<BulletParticleState>, CollisionDetector {
    override val body = CircleBody(
        initialPosition = initialPosition,
        initialRadius = 10f.sceneUnit,
    )
    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: AudioManager
    private lateinit var viewportManager: ViewportManager
    override val drawingOrder = 1f
    override val particleStateType = BulletParticleState::class
    override var particleEmissionMode: ParticleEmitter.Mode = ParticleEmitter.Mode.Continuous(
        emissionsPerMillisecond = 0.4f
    )
    override val collidableTypes = listOf(Ship::class)

    override fun createParticleState() = BulletParticleState(
        position = body.position,
        color = BulletColor,
    )

    override fun reuseParticleState(state: BulletParticleState) = state.reset(
        position = body.position,
        color = BulletColor,
    )

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

    override fun update(deltaTimeInMilliseconds: Int) {
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

    companion object {
        private val Speed = 6f.sceneUnit
        private val CollisionLimit = 64f.sceneUnit
        private val BulletColor = Color(0xffc29327)
    }
}