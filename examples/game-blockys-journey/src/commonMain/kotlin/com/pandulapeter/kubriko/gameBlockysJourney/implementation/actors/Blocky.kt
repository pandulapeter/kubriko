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
import com.pandulapeter.kubriko.helpers.Timer
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableCircleBody
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
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

internal class Blocky private constructor(
    state: State,
) : Visible, Dynamic, Editable<Blocky> {

    override val body = state.body
    private lateinit var spriteManager: SpriteManager
    private var direction = Direction.EAST
    private val animatedSprite = AnimatedSprite(
        getImageBitmap = { spriteManager.get(direction.drawableResource) },
        frameSize = IntSize(256, 256),
        frameCount = 40,
        framesPerRow = 8,
        framesPerSecond = 60f,
    )
    override val drawingOrder get() = -body.position.y.raw
    private val turningTimer = Timer(
        timeInMilliseconds = 650,
        shouldTriggerMultipleTimes = true,
        onDone = {
            direction = direction.nextDirectionClockwise
        }
    )

    override fun save() = State(
        body = body,
    )

    override fun onAdded(kubriko: Kubriko) {
        spriteManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        animatedSprite.stepForward(
            deltaTimeInMilliseconds = deltaTimeInMilliseconds,
            shouldLoop = true,
        )
        body.position += SceneOffset(
            x = +Speed * direction.angle.cos,
            y = -Speed * direction.angle.sin,
        ) * deltaTimeInMilliseconds
        turningTimer.update(deltaTimeInMilliseconds)
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

    private val Direction.nextDirectionClockwise
        get() = when (this) {
            Direction.EAST -> Direction.SOUTH_EAST
            Direction.SOUTH_EAST -> Direction.SOUTH
            Direction.SOUTH -> Direction.SOUTH_WEST
            Direction.SOUTH_WEST -> Direction.WEST
            Direction.WEST -> Direction.NORTH_WEST
            Direction.NORTH_WEST -> Direction.NORTH
            Direction.NORTH -> Direction.NORTH_EAST
            Direction.NORTH_EAST -> Direction.EAST
        }

    private val Direction.nextDirectionCounterClockwise
        get() = when (this) {
            Direction.EAST -> Direction.NORTH_EAST
            Direction.SOUTH_EAST -> Direction.EAST
            Direction.SOUTH -> Direction.SOUTH_EAST
            Direction.SOUTH_WEST -> Direction.SOUTH
            Direction.WEST -> Direction.SOUTH_WEST
            Direction.NORTH_WEST -> Direction.WEST
            Direction.NORTH -> Direction.NORTH_WEST
            Direction.NORTH_EAST -> Direction.NORTH
        }

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableCircleBody = CircleBody(),
    ) : Serializable.State<Blocky> {

        override fun restore() = Blocky(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        private val Speed = 0.25f.sceneUnit
    }
}