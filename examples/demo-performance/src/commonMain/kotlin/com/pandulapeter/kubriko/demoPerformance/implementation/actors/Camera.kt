/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializablePointBody
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

class Camera private constructor(state: State) : Unique, Dynamic, Positionable, Editable<Camera> {
    override val body = state.body

    private lateinit var actorManager: ActorManager
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        stateManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    private var acc = 0f

    override fun update(deltaTimeInMilliseconds: Int) {
        if (deltaTimeInMilliseconds > 0) {
            acc += deltaTimeInMilliseconds
            (acc / 5000f).rad.let { angle ->
                body.position = SceneOffset(
                    x = angle.cos.sceneUnit,
                    y = angle.sin.sceneUnit,
                ) * PATH_RADIUS
            }
            viewportManager.setCameraPosition(body.position)
        }
    }

    override fun save() = State(
        body = body,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializablePointBody = PointBody(),
    ) : Serializable.State<Camera> {

        override fun restore() = Camera(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        private const val PATH_RADIUS = 2500
    }
}
