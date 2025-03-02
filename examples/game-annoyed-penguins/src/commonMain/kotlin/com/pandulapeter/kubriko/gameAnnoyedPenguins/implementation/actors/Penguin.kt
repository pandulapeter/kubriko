/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableCircleBody
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_penguin
import kotlin.math.roundToInt
import kotlin.random.Random

internal class Penguin private constructor(
    state: State,
) : Visible, Editable<Penguin>, RigidBody, Dynamic {

    override val body: CircleBody = state.body
    private lateinit var spriteManager: SpriteManager
    private val animatedSprite = AnimatedSprite(
        getImageBitmap = { spriteManager.get(Res.drawable.sprite_penguin) },
        frameSize = IntSize(248, 256),
        frameCount = 2,
        framesPerRow = 2,
        framesPerSecond = 1f,
    )
    override val physicsBody = Body(
        shape = Circle(
            radius = body.radius * 0.8f,
        ),
        x = body.position.x,
        y = body.position.y * 1.2f,
    ).apply {
        restitution = 0.5f
        orientation = body.rotation
    }

    override fun onAdded(kubriko: Kubriko) {
        spriteManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
        body.rotation = physicsBody.orientation
        animatedSprite.stepForward(
            deltaTimeInMilliseconds = (deltaTimeInMilliseconds * (Random.nextFloat() * 5f)).roundToInt(),
            shouldLoop = true,
            speed = if (animatedSprite.isLastFrame) 2f else 0.3f,
        )
    }

    override fun DrawScope.draw() {
        try {
            animatedSprite.draw(this)
        } catch (_: RuntimeException) {
            // TODO: Happens in the editor
            drawCircle(
                color = Color.Black,
                radius = body.radius.raw,
                center = body.size.center.raw,
            )
        }
    }

    override fun save() = State(
        body = body
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableCircleBody = CircleBody(
            initialRadius = 128.sceneUnit,
        ),
    ) : Serializable.State<Penguin> {

        override fun restore() = Penguin(this)

        override fun serialize() = Json.encodeToString(this)
    }
}