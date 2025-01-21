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

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.directionTowards
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneSize
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_alien_ship
import kotlin.random.Random

internal class AlienShip : Visible, Dynamic {

    private lateinit var actorManager: ActorManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var metadataManager: MetadataManager
    override val body = RectangleBody(
        initialSize = SceneSize(
            width = 206.sceneUnit,
            height = 180.sceneUnit,
        ),
        initialScale = Scale.Unit * 0.75f,
    )
    private val animatedSprite = AnimatedSprite(
        getImageBitmap = { spriteManager.get(Res.drawable.sprite_alien_ship) },
        frameSize = IntSize(206, 180),
        frameCount = 91,
        framesPerRow = 9,
        framesPerSecond = 30f,
    )
    private var lastShotTimestamp = 0L

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        spriteManager = kubriko.get()
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        animatedSprite.stepForward(
            deltaTimeInMilliseconds = deltaTimeInMilliseconds,
            shouldLoop = true,
        )
        if (Random.nextInt(80) == 0) {
            val currentTimestamp = metadataManager.activeRuntimeInMilliseconds.value
            val timeSinceLastShot = currentTimestamp - lastShotTimestamp
            if (timeSinceLastShot > 200) {
                actorManager.allActors.value.filterIsInstance<Ship>().firstOrNull()?.let { ship ->
                    actorManager.add(
                        BulletAlien(
                            initialPosition = body.position,
                            direction = body.position.directionTowards(ship.body.position),
                        )
                    )
                    lastShotTimestamp = currentTimestamp
                }
            }
        }
    }

    override fun DrawScope.draw() = animatedSprite.draw(this)
}