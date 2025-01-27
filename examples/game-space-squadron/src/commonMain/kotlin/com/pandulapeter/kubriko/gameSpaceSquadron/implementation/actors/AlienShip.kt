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
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.extensions.directionTowards
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_alien_ship
import kotlin.random.Random

internal class AlienShip : Visible, Dynamic, Collidable {

    private lateinit var actorManager: ActorManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var viewportManager: ViewportManager
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
    private var speed = 0.25f

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        spriteManager = kubriko.get()
        metadataManager = kubriko.get()
        viewportManager = kubriko.get()
        resetPosition()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        animatedSprite.stepForward(
            deltaTimeInMilliseconds = deltaTimeInMilliseconds,
            shouldLoop = true,
        )
        if (body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager) && Random.nextInt(80) == 0) {
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
        body.position += SceneOffset.Down * speed * deltaTimeInMilliseconds
        if (body.position.y > viewportManager.bottomRight.value.y + body.size.height) {
            resetPosition()
        }
    }

    private fun resetPosition() {
        val left =  viewportManager.topLeft.value.x
        val right = viewportManager.bottomRight.value.x
        body.position = SceneOffset(
            x = left + (right - left) * Random.nextFloat(),
            y = viewportManager.topLeft.value.y - body.size.height,
        )
    }

    override fun DrawScope.draw() = animatedSprite.draw(this)
}