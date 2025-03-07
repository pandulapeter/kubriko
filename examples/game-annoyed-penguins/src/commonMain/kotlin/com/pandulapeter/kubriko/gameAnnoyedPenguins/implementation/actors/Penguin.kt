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

import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.BlinkingPenguin
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableCircleBody
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class Penguin private constructor(
    state: State,
) : BlinkingPenguin(
    body = state.body,
), Editable<Penguin>, RigidBody {

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

    override fun update(deltaTimeInMilliseconds: Int) {
        super.update(deltaTimeInMilliseconds)
        body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
        body.rotation = physicsBody.orientation
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