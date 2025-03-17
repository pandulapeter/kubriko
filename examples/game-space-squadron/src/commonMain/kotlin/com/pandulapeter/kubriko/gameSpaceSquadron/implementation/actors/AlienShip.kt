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
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.helpers.extensions.directionTowards
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
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
import kotlin.reflect.KClass

internal class AlienShip(
    private val initialY: SceneUnit,
) : Visible, Dynamic, Collidable, CollisionDetector {

    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: AudioManager
    private lateinit var gameplayManager: GameplayManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var stateManager: StateManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var viewportManager: ViewportManager
    override val body = BoxBody(
        initialSize = SceneSize(
            width = 206.sceneUnit,
            height = 180.sceneUnit,
        ),
        initialScale = Scale.Zero,
    )
    override val collisionMask = CircleCollisionMask(
        initialRadius = SceneUnit.Zero,
        initialPosition = body.position,
    )
    private val animatedSprite = AnimatedSprite(
        getImageBitmap = { spriteManager.get(Res.drawable.sprite_alien_ship) },
        frameSize = IntSize(206, 180),
        frameCount = 91,
        framesPerRow = 9,
        framesPerSecond = 30f,
    ).apply {
        frameIndex = Random.nextInt(frameCount)
    }
    private var lastShotTimestamp = 0L
    override val collidableTypes: List<KClass<out Collidable>> = listOf(AlienShip::class, Ship::class)
    var isShrinking = true
        private set

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        audioManager = kubriko.get()
        gameplayManager = kubriko.get()
        spriteManager = kubriko.get()
        stateManager = kubriko.get()
        metadataManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        collidables.filterIsInstance<Ship>().firstOrNull()?.onShipCollision() ?: resetPosition()
    }

    private fun Ship.onShipCollision() {
        if (!isShrinking) {
            onHit(true)
            actorManager.add(CameraShakeEffect())
            audioManager.playExplosionSmallSoundEffect()
            this@AlienShip.onHit(false)
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (stateManager.isRunning.value) {
            animatedSprite.stepForward(
                deltaTimeInMilliseconds = deltaTimeInMilliseconds,
                shouldLoop = true,
            )
            if (body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager) && Random.nextInt(80) == 0 && stateManager.isRunning.value) {
                val currentTimestamp = metadataManager.activeRuntimeInMilliseconds.value
                val timeSinceLastShot = currentTimestamp - lastShotTimestamp
                if (timeSinceLastShot > 200 && !gameplayManager.isGameOver.value) {
                    actorManager.allActors.value.filterIsInstance<Ship>().firstOrNull()?.let { ship ->
                        actorManager.add(
                            BulletEnemy(
                                initialPosition = body.position,
                                direction = body.position.directionTowards(ship.body.position),
                            )
                        )
                        lastShotTimestamp = currentTimestamp
                    }
                }
            }
            body.position += SceneOffset.Down * SPEED * deltaTimeInMilliseconds * gameplayManager.speedMultiplier.value
            if (body.position.y > viewportManager.bottomRight.value.y + body.size.height && !gameplayManager.isGameOver.value) {
                resetPosition()
            }
            if (isShrinking) {
                body.scale -= ShrinkingSpeed * deltaTimeInMilliseconds * gameplayManager.scaleMultiplier.value
                if (body.scale.horizontal <= 0f) {
                    resetPosition()
                }
            } else {
                if (gameplayManager.isGameOver.value) {
                    isShrinking = true
                }
            }
        }
        if (!isShrinking) {
            body.scale = StartingScale * gameplayManager.scaleMultiplier.value
        }
        collisionMask.position = body.position
        collisionMask.radius = CollisionMaskRadius * body.scale.horizontal
        collisionMask.pivot = collisionMask.size.center
    }

    fun onHit(canSpawnPowerup: Boolean) {
        if (!isShrinking) {
            isShrinking = true
            actorManager.add(
                Explosion(
                    position = body.position,
                    colors = listOf(Color(0xff748396), Color(0xfffdd461)),
                )
            )
            if (canSpawnPowerup) {
                if (Random.nextInt(10) == 5) {
                    actorManager.add(PowerUp(body.position))
                } else {
                    val isShipAtMaxHealth = actorManager.allActors.value.filterIsInstance<Ship>().firstOrNull()?.isShipAtMaxHealth == true
                    val arePowerUpsPresent = actorManager.allActors.value.filterIsInstance<Shield>().isNotEmpty()
                    if (Random.nextInt(10) == 5 && !isShipAtMaxHealth && !arePowerUpsPresent) {
                        actorManager.add(Shield(body.position))
                    }
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
        body.scale = StartingScale * gameplayManager.scaleMultiplier.value
        isShrinking = false
    }

    override fun DrawScope.draw() = animatedSprite.draw(this)

    companion object {
        private val CollisionMaskRadius = 90.sceneUnit
        private val StartingScale = Scale.Unit * 0.75f
        private val ShrinkingSpeed = Scale.Unit * 0.004f
        private const val SPEED = 0.25f
    }
}