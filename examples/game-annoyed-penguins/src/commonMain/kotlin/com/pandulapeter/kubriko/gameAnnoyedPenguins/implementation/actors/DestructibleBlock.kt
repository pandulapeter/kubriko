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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.DestructiblePhysicsObject
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class DestructibleBlock private constructor(
    state: State,
) : DestructiblePhysicsObject<DestructibleBlock>() {

    override val body = state.body
    override val collisionMask = BoxCollisionMask(
        initialSize = body.size * body.scale,
        initialPosition = body.position,
        initialRotation = body.rotation,
    )
    override val physicsBody = PhysicsBody(collisionMask).apply {
        restitution = state.restitution
        density = state.density
        rotation = body.rotation
        staticFriction = 0.01f
    }
    @set:Exposed(name = "restitution")
    var restitution
        get() = physicsBody.restitution
        set(value) {
            physicsBody.restitution = value
        }
    @set:Exposed(name = "density")
    var density
        get() = physicsBody.density
        set(value) {
            physicsBody.density = value
        }

    @set:Exposed(name = "color")
    var color: Color = state.color

    override fun DrawScope.draw() {
        drawRect(
            color = color.copy(alpha = alpha),
            size = body.size.raw,
        )
        drawRect(
            color = lerp(color, Color.Black, 0.25f).copy(alpha = alpha),
            size = body.size.raw,
            style = Stroke(width = 16f),
        )
    }

    override fun save() = State(
        body = body,
        color = color,
        restitution = restitution,
        density = density,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
        @SerialName("color") val color: SerializableColor = Color.White,
        @SerialName("restitution") val restitution: Float = 1f,
        @SerialName("density") val density: Float = 10f,
    ) : Serializable.State<DestructibleBlock> {

        override fun restore() = DestructibleBlock(this)

        override fun serialize() = Json.encodeToString(this)
    }
}