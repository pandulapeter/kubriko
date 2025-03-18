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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class StaticBox private constructor(state: State) : RigidBody, Visible, Dynamic, Editable<StaticBox> {
    override val body = state.body
    override val physicsBody = PhysicsBody(
        shape = Polygon(body.size.width / 2f, body.size.height / 2f),
        position = body.position,
    ).apply {
        density = 0f
        orientation = body.rotation
    }
    override val collisionMask = BoxCollisionMask(
        initialSize = body.size * body.scale,
        initialPosition = body.position,
        initialRotation = body.rotation,
    )

    @set:Exposed(name = "isRotating")
    var isRotating = state.isRotating

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isRotating) {
            body.rotation -= (0.002 * deltaTimeInMilliseconds).toFloat().rad
            physicsBody.orientation = body.rotation
            collisionMask.rotation = body.rotation
        }
    }

    override fun DrawScope.draw() {
        drawRect(
            color = Color.DarkGray,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(),
        )
    }

    override fun save() = State(
        body = body,
        isRotating = isRotating,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
        @SerialName("isRotating") val isRotating: Boolean = false,
    ) : Serializable.State<StaticBox> {

        override fun restore() = StaticBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
