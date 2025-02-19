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
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.base.Bullet
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

internal class BulletPlayer(
    initialPosition: SceneOffset,
    direction: AngleRadians,
) : Bullet(
    initialPosition = initialPosition,
    direction = direction,
    playSoundEffect = { playShootSoundEffect() },
    bulletColor = Color(0xff5199a6),
    bulletBaseSpeed = 1.sceneUnit,
) {
    override val collidableTypes = listOf(AlienShip::class)

    override fun onCollisionDetected(collidables: List<Collidable>) {
        var isPlayingExplosion = false
        collidables.filterIsInstance<AlienShip>().filterNot { it.isShrinking }.forEach { alienShip ->
            actorManager.remove(this)
            if (!isPlayingExplosion) {
                audioManager.playExplosionSmallSoundEffect()
                isPlayingExplosion = true
            }
            alienShip.onHit(true)
        }
    }
}