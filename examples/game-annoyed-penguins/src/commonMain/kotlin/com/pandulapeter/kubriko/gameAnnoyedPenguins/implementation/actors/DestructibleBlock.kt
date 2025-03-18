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
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.DestructiblePhysicsObject
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
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
    override val physicsBody = PhysicsBody(
        shape = Polygon(
            halfWidth = body.size.width / 2,
            halfHeight = body.size.height / 2,
        ),
        x = body.position.x,
        y = body.position.y,
    ).apply {
        restitution = 1f
        density = 10f
        orientation = body.rotation
    }

    @set:Exposed(name = "color")
    var color: Color = state.color

    override fun DrawScope.draw() {
        drawRect(
            color = color,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(width = 6f),
        )
    }

    override fun save() = State(
        body = body,
        color = color,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
        @SerialName("color") val color: SerializableColor = Color.White,
    ) : Serializable.State<DestructibleBlock> {

        override fun restore() = DestructibleBlock(this)

        override fun serialize() = Json.encodeToString(this)
    }
}