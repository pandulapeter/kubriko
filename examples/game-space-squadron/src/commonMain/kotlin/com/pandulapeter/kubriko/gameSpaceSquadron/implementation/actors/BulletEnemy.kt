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
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.base.Bullet
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

internal class BulletEnemy(
    initialPosition: SceneOffset,
    direction: AngleRadians,
) : Bullet(
    initialPosition = initialPosition,
    direction = direction,
    playSoundEffect = { playShootAlienSoundEffect() },
    bulletColor = Color(0xffc29327),
    bulletBaseSpeed = 0.5f.sceneUnit,
    speedIncrement = { 1 + it * 0.05f },
) {
    override val collidableTypes = listOf(Ship::class)

    override fun onCollisionDetected(collidables: List<Collidable>) {
        collidables.filterIsInstance<Ship>().firstOrNull()?.let { ship ->
            if (body.position.distanceTo(ship.body.position) < CollisionLimit) {
                audioManager.playShipHitSoundEffect()
                actorManager.add(CameraShakeEffect())
                actorManager.remove(this)
                ship.onHit(false)
            }
        }
    }

    companion object {
        private val CollisionLimit = 64f.sceneUnit
    }
}