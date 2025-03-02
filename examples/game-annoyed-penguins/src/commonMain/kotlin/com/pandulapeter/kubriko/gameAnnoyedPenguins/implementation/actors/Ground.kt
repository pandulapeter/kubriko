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
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableRectangleBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class Ground private constructor(state: State) : RigidBody, Visible, Editable<Ground> {
    override val body = state.body
    override val physicsBody = Body(
        shape = Polygon(body.size.width / 2f, body.size.height / 2f),
        x = body.position.x,
        y = body.position.y,
    ).apply {
        density = 0f
        orientation = body.rotation
    }

    @set:Exposed(name = "color")
    var color: Color = state.color

    override fun onAdded(kubriko: Kubriko) {
        physicsBody.orientation = body.rotation
    }

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
        @SerialName("body") val body: SerializableRectangleBody = RectangleBody(),
        @SerialName("color") val color: SerializableColor = Color.LightGray,
    ) : Serializable.State<Ground> {

        override fun restore() = Ground(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
