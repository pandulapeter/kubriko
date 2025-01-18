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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.InsetPaddingAware
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.abs
import com.pandulapeter.kubriko.extensions.distanceTo
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.min
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.toSceneOffset
import com.pandulapeter.kubriko.gameSpaceSquadron.ViewportHeight
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.collections.immutable.ImmutableSet
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_ship
import kotlin.math.abs
import kotlin.math.hypot

internal class Ship : Visible, Dynamic, InsetPaddingAware, Group, KeyboardInputAware, PointerInputAware {

    private lateinit var actorManager: ActorManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var viewportManager: ViewportManager
    private val shipAnimationWrapper by lazy { ShipAnimationWrapper(spriteManager) }
    override val body = RectangleBody(
        initialSize = SceneSize(
            width = 128.sceneUnit,
            height = 144.sceneUnit,
        ),
    )
    private val shipDestination = ShipDestination()
    override val actors = listOf(shipDestination)

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        spriteManager = kubriko.get()
        metadataManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun onInsetPaddingChanged(insetPadding: Rect) {
        // TODO: Limit the play area
        val topLeft = insetPadding.topLeft.toSceneOffset(viewportManager)
        val bottomRight = insetPadding.bottomRight.toSceneOffset(viewportManager)
        val offset = SceneOffset(
            x = SceneUnit.Zero,
            y = body.pivot.y * 4,
        )
        body.position = SceneOffset(
            x = (topLeft.x - bottomRight.x) / 2,
            y = (topLeft.y - bottomRight.y) / 2 + ViewportHeight / 2,
        ) - offset
    }

    override fun handleActivePointers(screenOffset: Offset) = shoot()

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        if (Key.Spacebar in activeKeys) {
            shoot()
        }
    }

    private var lastShotTimestamp = 0L

    private fun shoot() {
        val currentTimestamp = metadataManager.activeRuntimeInMilliseconds.value
        val timeSinceLastShot = currentTimestamp - lastShotTimestamp
        if (timeSinceLastShot > 200) {
            lastShotTimestamp = currentTimestamp
            actorManager.add(Bullet(body.position))
        }
    }

    private var speed = SceneUnit.Zero

    override fun update(deltaTimeInMilliseconds: Float) {
        val previousX = body.position.x
        speed = min(shipDestination.body.position.distanceTo(body.position) * 0.03f + 0.5f.sceneUnit, MaxSpeed)
        // TODO: Implement momentum
        moveTowards(shipDestination.body.position, speed)
        shipAnimationWrapper.update(deltaTimeInMilliseconds, previousX, body.position.x)
        body.scale = Scale(shipAnimationWrapper.horizontalScale, 1f)
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
        spriteManager: SpriteManager
    ) {
        private val animatedSprite = AnimatedSprite(
            getImageBitmap = { spriteManager.get(Res.drawable.sprite_ship) },
            frameSize = IntSize(128, 144),
            frameCount = 23,
            framesPerRow = 8,
            framesPerSecond = 60f,
        )

        var horizontalScale = 1f
            private set

        fun update(deltaTimeInMilliseconds: Float, previousX: SceneUnit, currentX: SceneUnit) {
            val distance = (previousX - currentX).abs
            val animationSpeed = deltaTimeInMilliseconds * distance.raw * 0.2f
            if (distance < MinDistanceForAnimation) {
                // Not moving
                if (!animatedSprite.isFirstFrame) {
                    animatedSprite.stepBackwards(deltaTimeInMilliseconds * 0.8f)
                }
            } else if (currentX < previousX) {
                // Moving left
                if (horizontalScale == -1f) {
                    if (animatedSprite.isFirstFrame) {
                        horizontalScale = 1f
                    } else {
                        animatedSprite.stepBackwards(animationSpeed)
                    }
                } else {
                    if (!animatedSprite.isLastFrame) {
                        animatedSprite.stepForward(animationSpeed)
                    }
                }
            } else if (currentX > previousX) {
                // Moving right
                if (horizontalScale == 1f) {
                    if (animatedSprite.isFirstFrame) {
                        horizontalScale = -1f
                    } else {
                        animatedSprite.stepBackwards(animationSpeed)
                    }
                } else {
                    if (!animatedSprite.isLastFrame) {
                        animatedSprite.stepForward(animationSpeed)
                    }
                }
            }
        }

        fun draw(drawScope: DrawScope) = animatedSprite.draw(drawScope)
    }

    companion object {
        private val MaxSpeed = 16.sceneUnit
        private val MinDistanceForAnimation = 3.sceneUnit
    }
}