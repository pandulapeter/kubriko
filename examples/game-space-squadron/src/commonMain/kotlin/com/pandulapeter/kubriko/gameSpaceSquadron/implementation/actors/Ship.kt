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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.extensions.abs
import com.pandulapeter.kubriko.extensions.distanceTo
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.min
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.collections.immutable.ImmutableSet
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_ship
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

internal class Ship : Visible, Dynamic, Group, KeyboardInputAware, PointerInputAware, Collidable {

    private lateinit var actorManager: ActorManager
    private lateinit var gameplayManager: GameplayManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var stateManager: StateManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var viewportManager: ViewportManager
    override val body = RectangleBody(
        initialSize = SceneSize(
            width = 237.sceneUnit,
            height = 341.sceneUnit,
        ),
        initialScale = Scale.Unit * 0.5f,
    )
    private val shipAnimationWrapper by lazy {
        ShipAnimationWrapper(
            spriteManager = spriteManager,
            initialScale = body.scale,
        )
    }
    private val shipDestination = ShipDestination()
    override val actors = listOf(shipDestination)
    private var lastShotTimestamp = 0L
    private var remainingMultiShootCount = 0

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        gameplayManager = kubriko.get()
        spriteManager = kubriko.get()
        stateManager = kubriko.get()
        metadataManager = kubriko.get()
        viewportManager = kubriko.get()
        body.position = SceneOffset(
            x = SceneUnit.Zero,
            y = viewportManager.bottomRight.value.y + body.size.height,
        )
    }

    fun onPowerUpCollected() {
        remainingMultiShootCount += 10
    }

    override fun onRemoved() = gameplayManager.onGameOver()

    override fun handleActivePointers(screenOffset: Offset) = shoot()

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        if (Key.Spacebar in activeKeys) {
            shoot()
        }
    }

    private fun shoot() {
        if (stateManager.isRunning.value) {
            val currentTimestamp = metadataManager.activeRuntimeInMilliseconds.value
            val timeSinceLastShot = currentTimestamp - lastShotTimestamp
            if (timeSinceLastShot > 250) {
                lastShotTimestamp = currentTimestamp
                if (remainingMultiShootCount > 0) {
                    remainingMultiShootCount -= 1
                    actorManager.add(
                        Bullet(
                            initialPosition = body.position,
                            directionOffset = AngleRadians.Zero,
                        ),
                        Bullet(
                            initialPosition = body.position,
                            directionOffset = AngleRadians.Pi / 24,
                        ),
                        Bullet(
                            initialPosition = body.position,
                            directionOffset = -AngleRadians.Pi / 24,
                        ),
                        Bullet(
                            initialPosition = body.position,
                            directionOffset = AngleRadians.Pi / 12,
                        ),
                        Bullet(
                            initialPosition = body.position,
                            directionOffset = -AngleRadians.Pi / 12,
                        ),
                    )
                } else {
                    actorManager.add(
                        Bullet(
                            initialPosition = body.position,
                            directionOffset = AngleRadians.Zero,
                        )
                    )
                }
            }
        }
    }

    private var speed = SceneUnit.Zero

    override fun update(deltaTimeInMilliseconds: Int) {
        val previousX = body.position.x
        speed = min(shipDestination.body.position.distanceTo(body.position) * 0.01f + 0.5f.sceneUnit, MaxSpeed) * deltaTimeInMilliseconds
        // TODO: Implement momentum
        moveTowards(shipDestination.body.position, speed)
        shipAnimationWrapper.update(deltaTimeInMilliseconds, previousX, body.position.x)
        body.scale = Scale(shipAnimationWrapper.horizontalScale, shipAnimationWrapper.verticalScale)
    }

    private fun moveTowards(target: SceneOffset, speed: SceneUnit) {
        val deltaX = target.x - body.position.x
        val deltaY = target.y - body.position.y
        val distance = hypot(deltaX.raw, deltaY.raw).sceneUnit
        body.position = if (distance <= speed) {
            target
        } else {
            val absDeltaX = abs(deltaX.raw).sceneUnit
            val absDeltaY = abs(deltaY.raw).sceneUnit
            var x = body.position.x
            var y = body.position.y
            if (absDeltaX > absDeltaY) {
                val ratio = absDeltaY / absDeltaX
                x += if (deltaX > SceneUnit.Zero) speed else -speed
                y += if (deltaY > SceneUnit.Zero) speed * ratio else -speed * ratio
            } else {
                val ratio = absDeltaX / absDeltaY
                y += if (deltaY > SceneUnit.Zero) speed else -speed
                x += if (deltaX > SceneUnit.Zero) speed * ratio else -speed * ratio
            }
            SceneOffset(x, y)
        }
    }

    override fun DrawScope.draw() = shipAnimationWrapper.draw(this)

    private class ShipAnimationWrapper(
        spriteManager: SpriteManager,
        private val initialScale: Scale,
    ) {
        private val animatedSprite = AnimatedSprite(
            getImageBitmap = { spriteManager.get(Res.drawable.sprite_ship) },
            frameSize = IntSize(237, 341),
            frameCount = 32,
            framesPerRow = 8,
            framesPerSecond = 60f,
        )

        var horizontalScale = initialScale.horizontal
            private set
        val verticalScale = initialScale.vertical

        fun update(deltaTimeInMilliseconds: Int, previousX: SceneUnit, currentX: SceneUnit) {
            val distance = (previousX - currentX).abs
            val animationSpeed = (deltaTimeInMilliseconds * distance.raw * 0.2f).roundToInt()
            if (distance < MinDistanceForAnimation) {
                // Not moving
                if (!animatedSprite.isFirstFrame) {
                    animatedSprite.stepBackwards(
                        deltaTimeInMilliseconds = deltaTimeInMilliseconds,
                        speed = 2f,
                    )
                }
            } else if (currentX < previousX) {
                // Moving left
                if (horizontalScale < 0) {
                    if (animatedSprite.isFirstFrame) {
                        horizontalScale = initialScale.horizontal
                    } else {
                        animatedSprite.stepBackwards(
                            deltaTimeInMilliseconds = animationSpeed,
                            speed = 0.5f,
                        )
                    }
                } else {
                    if (!animatedSprite.isLastFrame) {
                        animatedSprite.stepForward(animationSpeed)
                    }
                }
            } else if (currentX > previousX) {
                // Moving right
                if (horizontalScale > 0) {
                    if (animatedSprite.isFirstFrame) {
                        horizontalScale = -initialScale.horizontal
                    } else {
                        animatedSprite.stepBackwards(
                            deltaTimeInMilliseconds = animationSpeed,
                            speed = 0.5f,
                        )
                    }
                } else {
                    if (!animatedSprite.isLastFrame) {
                        animatedSprite.stepForward(
                            deltaTimeInMilliseconds = animationSpeed,
                            speed = 2f,
                        )
                    }
                }
            }
        }

        fun draw(drawScope: DrawScope) = animatedSprite.draw(drawScope)
    }

    companion object {
        private val MaxSpeed = 3.sceneUnit
        private val MinDistanceForAnimation = 3.sceneUnit
    }
}