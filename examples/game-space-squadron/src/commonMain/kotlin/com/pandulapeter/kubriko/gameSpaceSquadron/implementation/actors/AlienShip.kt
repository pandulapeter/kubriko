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
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.extensions.directionTowards
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_alien_ship
import kotlin.random.Random

internal class AlienShip(
    private val initialY: SceneUnit,
) : Visible, Dynamic, Collidable, CollisionDetector {

    private lateinit var actorManager: ActorManager
    private lateinit var gameplayManager: GameplayManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var stateManager: StateManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var viewportManager: ViewportManager
    override val body = RectangleBody(
        initialSize = SceneSize(
            width = 206.sceneUnit,
            height = 180.sceneUnit,
        ),
        initialScale = StartingScale,
    )
    override val collisionBody = CircleBody(
        initialRadius = 70.sceneUnit,
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
    override val collidableTypes = listOf(AlienShip::class)
    var isShrinking = false
        private set

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        gameplayManager = kubriko.get()
        spriteManager = kubriko.get()
        stateManager = kubriko.get()
        metadataManager = kubriko.get()
        viewportManager = kubriko.get()
        resetPosition()
    }

    override fun onCollisionDetected(collidables: List<Collidable>) = resetPosition()

    override fun update(deltaTimeInMilliseconds: Int) {
        animatedSprite.stepForward(
            deltaTimeInMilliseconds = deltaTimeInMilliseconds,
            shouldLoop = true,
        )
        if (body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager) && Random.nextInt(80) == 0) {
            val currentTimestamp = metadataManager.activeRuntimeInMilliseconds.value
            val timeSinceLastShot = currentTimestamp - lastShotTimestamp
            if (timeSinceLastShot > 200 && !gameplayManager.isGameOver) {
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
        if (isShrinking) {
            body.scale -= ShrinkingSpeed * deltaTimeInMilliseconds
            if (body.scale.horizontal <= 0f) {
                resetPosition()
            }
        }
        collisionBody.position = body.position
    }

    fun onHit() {
        if (!isShrinking) {
            isShrinking = true
            actorManager.add(
                Explosion(
                    position = body.position,
                    colors = listOf(Color(0xff748396), Color(0xfffdd461)),
                )
            )
            if (Random.nextInt(20) == 5) {
                actorManager.add(PowerUp(body.position))
            } else {
                if (Random.nextInt(20) == 5) {
                    actorManager.add(Shield(body.position))
                }
            }
        }
    }

    private fun resetPosition() {
        val left = viewportManager.topLeft.value.x
        val right = viewportManager.bottomRight.value.x
        body.position = SceneOffset(
            x = left + (right - left) * Random.nextFloat(),
            y = viewportManager.topLeft.value.y - body.size.height - initialY,
        )
        body.scale = StartingScale
        isShrinking = false
        if (gameplayManager.isGameOver && stateManager.isRunning.value) {
            gameplayManager.pauseGame()
        }
    }

    override fun DrawScope.draw() = animatedSprite.draw(this)

    companion object {
        private val StartingScale = Scale.Unit * 0.75f
        private val ShrinkingSpeed = Scale(
            horizontal = 0.005f,
            vertical = 0.005f,
        )
    }
}