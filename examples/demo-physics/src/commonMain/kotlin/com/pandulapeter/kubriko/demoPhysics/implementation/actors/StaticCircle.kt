/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableCircleBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class StaticCircle private constructor(state: State) : RigidBody, Visible, Editable<StaticCircle> {
    override val body = state.body
    override val physicsBody = Body(
        shape = Circle(body.radius),
        x = body.position.x,
        y = body.position.y
    ).apply { density = 0f }
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }

    override fun DrawScope.draw() {
        drawCircle(
            color = Color.DarkGray,
            radius = body.radius.raw,
            center = body.size.center.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = body.radius.raw,
            center = body.size.center.raw,
            style = Stroke(),
        )
    }

    override fun save() = State(
        body = body,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableCircleBody
    ) : Serializable.State<StaticCircle> {

        override fun restore() = StaticCircle(this)

        override fun serialize() = Json.encodeToString(this)
    }
}