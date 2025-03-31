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
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.FadingActor
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class Ground private constructor(state: State) : FadingActor(), RigidBody, Visible, Editable<Ground> {
    override val body = state.body
    override val collisionMask = BoxCollisionMask(
        initialSize = body.size * body.scale,
        initialPosition = body.position,
        initialRotation = body.rotation,
    )
    override val physicsBody = PhysicsBody(collisionMask).apply {
        density = 0f
        rotation = body.rotation
    }
    override val drawingOrder = -2f

    @set:Exposed(name = "color")
    var color: Color = state.color

    override fun onAdded(kubriko: Kubriko) {
        physicsBody.rotation = body.rotation
    }

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
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
        @SerialName("color") val color: SerializableColor = Color.LightGray,
    ) : Serializable.State<Ground> {

        override fun restore() = Ground(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
