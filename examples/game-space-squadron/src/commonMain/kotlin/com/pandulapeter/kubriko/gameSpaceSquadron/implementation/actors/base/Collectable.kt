/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.base

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.extensions.distanceTo
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.Ship
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import org.jetbrains.compose.resources.DrawableResource

internal abstract class Collectable(
    position: SceneOffset,
    spriteSheet: DrawableResource,
    frameSize: IntSize,
    frameCount: Int,
    framesPerRow: Int,
) : Visible, Dynamic, CollisionDetector {

    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: AudioManager
    private lateinit var gameplayManager: GameplayManager
    private lateinit var stateManager: StateManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var viewportManager: ViewportManager
    override val body = RectangleBody(
        initialSize = SceneSize(frameSize.width.sceneUnit, frameSize.height.sceneUnit),
        initialPosition = position,
    )
    override val collisionBody = CircleBody(
        initialRadius = 20.sceneUnit,
    )
    private val animatedSprite = AnimatedSprite(
        getImageBitmap = { spriteManager.get(spriteSheet) },
        frameSize = frameSize,
        frameCount = frameCount,
        framesPerRow = framesPerRow,
        framesPerSecond = 30f,
    )
    override val collidableTypes = listOf(Ship::class)
    override val drawingOrder = 2f
    private var isShrinking = false

    protected abstract fun Ship.onCollected()

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        audioManager = kubriko.get()
        gameplayManager = kubriko.get()
        stateManager = kubriko.get()
        spriteManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun DrawScope.draw() = animatedSprite.draw(this)

    override fun update(deltaTimeInMilliseconds: Int) {
        if (stateManager.isRunning.value) {
            animatedSprite.stepForward(
                deltaTimeInMilliseconds = deltaTimeInMilliseconds,
                shouldLoop = true,
            )
            body.position += SceneOffset.Down * SPEED * deltaTimeInMilliseconds * gameplayManager.speedMultiplier.value
            if (body.position.y > viewportManager.bottomRight.value.y + body.size.height) {
                actorManager.remove(this)
            }
        }
        collisionBody.position = body.position
        collisionBody.scale = body.scale
        if (isShrinking) {
            if (body.scale.horizontal <= 0) {
                actorManager.remove(this)
            } else {
                body.scale -= Scale.Unit * 0.003f * deltaTimeInMilliseconds * gameplayManager.scaleMultiplier.value
            }
        } else {
            body.scale = StartingScale * gameplayManager.scaleMultiplier.value
        }
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        if (!isShrinking) {
            collidables.filterIsInstance<Ship>().firstOrNull()?.let { ship ->
                if (body.position.distanceTo(ship.body.position) < CollisionLimit) {
                    ship.onCollected()
                    isShrinking = true
                    audioManager.playPowerUpSoundEffect()
                }
            }
        }
    }

    companion object {
        private const val SPEED = 0.3f
        private val CollisionLimit = 72f.sceneUnit
        private val StartingScale = Scale.Unit * 0.4f
    }
}