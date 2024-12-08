package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.implementation.extensions.cos
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.sin
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneOffset
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Character private constructor(state: State) : Unique, Dynamic, Positionable, Editable<Character> {

    override val body = CircleBody(
        initialPosition = state.position,
        initialRadius = 50f.sceneUnit,
    )
    private lateinit var actorManager: ActorManager
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.require()
        stateManager = kubriko.require()
        viewportManager = kubriko.require()
    }

    private var acc = 0f

    override fun update(deltaTimeInMillis: Float) {
        acc += deltaTimeInMillis
        (acc / 10000f).rad.let { angle ->
            body.position = SceneOffset(
                x = angle.cos.sceneUnit,
                y = angle.sin.sceneUnit,
            ) * PATH_RADIUS
        }
        viewportManager.setCameraPosition(body.position)
    }

    override fun save() = State(position = body.position)

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("position") val position: SerializableSceneOffset = SceneOffset.Zero,
    ) : Serializable.State<Character> {

        override fun restore() = Character(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        private const val VIEWPORT_FOLLOWING_SPEED_MULTIPLIER = 0.03f
        private const val PATH_RADIUS = 5000
    }
}
