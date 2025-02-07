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
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.extensions.distanceTo
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_power_up

internal class PowerUp(
    position: SceneOffset,
) : Visible, Dynamic, CollisionDetector {

    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: AudioManager
    private lateinit var gameplayManager: GameplayManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var viewportManager: ViewportManager
    override val body = RectangleBody(
        initialSize = SceneSize(
            width = 198.sceneUnit,
            height = 186.sceneUnit,
        ),
        initialPosition = position,
        initialScale = Scale.Unit * 0.4f
    )
    override val collisionBody = CircleBody(
        initialRadius = 20.sceneUnit,
    )
    private val animatedSprite = AnimatedSprite(
        getImageBitmap = { spriteManager.get(Res.drawable.sprite_power_up) },
        frameSize = IntSize(198, 186),
        frameCount = 87,
        framesPerRow = 10,
        framesPerSecond = 30f,
    )
    override val collidableTypes = listOf(Ship::class)

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        audioManager = kubriko.get()
        gameplayManager = kubriko.get()
        spriteManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun DrawScope.draw() = animatedSprite.draw(this)

    override fun update(deltaTimeInMilliseconds: Int) {
        animatedSprite.stepForward(
            deltaTimeInMilliseconds = deltaTimeInMilliseconds,
            shouldLoop = true,
        )
        body.position += SceneOffset.Down * SPEED * deltaTimeInMilliseconds
        if (body.position.y > viewportManager.bottomRight.value.y + body.size.height) {
            actorManager.remove(this)
        }
        collisionBody.position = body.position
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        collidables.filterIsInstance<Ship>().firstOrNull()?.let { ship ->
            if (body.position.distanceTo(ship.body.position) < CollisionLimit) {
                ship.onPowerUpCollected()
                audioManager.playPowerUpSoundEffect()
                actorManager.remove(this)
            }
        }
    }

    companion object {
        private const val SPEED = 0.3f
        private val CollisionLimit = 64f.sceneUnit
    }
}