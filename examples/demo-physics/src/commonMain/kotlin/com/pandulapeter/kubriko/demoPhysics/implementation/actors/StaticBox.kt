package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneOffset
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneSize
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class StaticBox private constructor(
    private val state: State,
) : RigidBody, Visible, Dynamic, Editable<StaticBox> {
    override val physicsBody = Body(
        shape = Polygon(state.size.width / 2f, state.size.height / 2f),
        x = state.initialOffset.x,
        y = state.initialOffset.y,
    ).apply { density = 0f }
    override val body = RectangleBody(
        initialPosition = state.initialOffset,
        initialSize = state.size,
    )

    override fun update(deltaTimeInMillis: Float) {
        if (state.isRotating) {
            body.rotation -= (0.002 * deltaTimeInMillis).toFloat().rad
            physicsBody.orientation = body.rotation
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
        initialOffset = body.position,
        size = body.size,
        isRotating = state.isRotating,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("initialOffset") val initialOffset: SerializableSceneOffset = SceneOffset.Zero,
        @SerialName("size") val size: SerializableSceneSize = SceneSize.Zero,
        @SerialName("isRotating") val isRotating: Boolean = false,
    ) : Serializable.State<StaticBox> {

        override fun restore() = StaticBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
