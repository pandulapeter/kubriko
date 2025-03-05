/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.actors

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.deg
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.rad
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import kubriko.examples.game_blockys_journey.generated.resources.Res
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_east
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_north
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_north_east
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_north_west
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_south
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_south_east
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_south_west
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_west
import org.jetbrains.compose.resources.DrawableResource

internal class Blocky : Visible, Dynamic {

    override val body = CircleBody(
        initialRadius = 128.sceneUnit,
    )
    private lateinit var spriteManager: SpriteManager
    private var direction = Direction.EAST
    private val animatedSprite = AnimatedSprite(
        getImageBitmap = { spriteManager.get(direction.drawableResource) },
        frameSize = IntSize(256, 256),
        frameCount = 40,
        framesPerRow = 8,
        framesPerSecond = 60f,
    )

    override fun onAdded(kubriko: Kubriko) {
        spriteManager = kubriko.get()
    }

    private var previousFrame = 0

    override fun update(deltaTimeInMilliseconds: Int) {
        animatedSprite.stepForward(
            deltaTimeInMilliseconds = deltaTimeInMilliseconds,
            shouldLoop = true,
        )
        body.position += SceneOffset(
            x = + Speed * direction.angle.cos,
            y = - Speed * direction.angle.sin,
        ) * deltaTimeInMilliseconds
        if (previousFrame != animatedSprite.frameIndex && animatedSprite.isLastFrame) {
            direction = when (direction) {
                Direction.EAST -> Direction.SOUTH_EAST
                Direction.SOUTH_EAST -> Direction.SOUTH
                Direction.SOUTH -> Direction.SOUTH_WEST
                Direction.SOUTH_WEST -> Direction.WEST
                Direction.WEST -> Direction.NORTH_WEST
                Direction.NORTH_WEST -> Direction.NORTH
                Direction.NORTH -> Direction.NORTH_EAST
                Direction.NORTH_EAST -> Direction.EAST
            }
        }
        previousFrame = animatedSprite.frameIndex
    }

    override fun DrawScope.draw() = animatedSprite.draw(this)

    private enum class Direction(
        val drawableResource: DrawableResource,
        val angle: AngleRadians,
    ) {
        EAST(
            drawableResource = Res.drawable.sprite_blocky_east,
            angle = 0.deg.rad,
        ),
        SOUTH_EAST(
            drawableResource = Res.drawable.sprite_blocky_south_east,
            angle = 330.deg.rad,
        ),
        SOUTH(
            drawableResource = Res.drawable.sprite_blocky_south,
            angle = 270.deg.rad,
        ),
        SOUTH_WEST(
            drawableResource = Res.drawable.sprite_blocky_south_west,
            angle = 210.deg.rad,
        ),
        WEST(
            drawableResource = Res.drawable.sprite_blocky_west,
            angle = 180.deg.rad,
        ),
        NORTH_WEST(
            drawableResource = Res.drawable.sprite_blocky_north_west,
            angle = 150.deg.rad,
        ),
        NORTH(
            drawableResource = Res.drawable.sprite_blocky_north,
            angle = 90.deg.rad,
        ),
        NORTH_EAST(
            drawableResource = Res.drawable.sprite_blocky_north_east,
            angle = 30.deg.rad,
        ),
    }

    companion object {
        private val Speed = 0.25f.sceneUnit
    }
}