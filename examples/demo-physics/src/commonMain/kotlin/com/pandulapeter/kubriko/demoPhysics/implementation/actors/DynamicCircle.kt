/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.physics.implementation.dynamics.PhysicsBody
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class DynamicCircle private constructor(state: State) : BaseDynamicObject(), Editable<DynamicCircle> {
    override val body = state.body
    val radius = body.size.width / 2f
    override val physicsBody = PhysicsBody(
        shape = Circle(radius),
        x = body.position.x,
        y = body.position.y,
    ).apply {
        restitution = 1f
        orientation = body.rotation
    }
    override val collisionMask = CircleCollisionMask(
        initialRadius = radius,
        initialPosition = body.position,
    )

    override fun DrawScope.draw() {
        drawCircle(
            color = color,
            radius = radius.raw,
            center = body.size.center.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = radius.raw,
            center = body.size.center.raw,
            style = Stroke(),
        )
        drawLine(
            color = Color.Black,
            start = Offset(0f, radius.raw),
            end = Offset(body.size.width.raw, radius.raw),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Black,
            start = Offset(radius.raw, 0f),
            end = Offset(radius.raw, body.size.height.raw),
            strokeWidth = 2f,
        )
    }

    override fun save() = State(
        body = body,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
    ) : Serializable.State<DynamicCircle> {

        override fun restore() = DynamicCircle(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
