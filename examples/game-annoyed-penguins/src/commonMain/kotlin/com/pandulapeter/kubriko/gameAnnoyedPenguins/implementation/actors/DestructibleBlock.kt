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
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.DestructiblePhysicsObject
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableRectangleBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class DestructibleBlock private constructor(
    state: State,
) : DestructiblePhysicsObject<DestructibleBlock>() {

    override val body = state.body
    override val physicsBody = Body(
        shape = Polygon(
            halfWidth = body.size.width / 2,
            halfHeight = body.size.height / 2,
        ),
        x = body.position.x,
        y = body.position.y,
    ).apply {
        restitution = 0.5f
        orientation = body.rotation
    }

    override fun DrawScope.draw() {
        drawRect(
            color = Color.Cyan,
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
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableRectangleBody = RectangleBody(),
    ) : Serializable.State<DestructibleBlock> {

        override fun restore() = DestructibleBlock(this)

        override fun serialize() = Json.encodeToString(this)
    }
}