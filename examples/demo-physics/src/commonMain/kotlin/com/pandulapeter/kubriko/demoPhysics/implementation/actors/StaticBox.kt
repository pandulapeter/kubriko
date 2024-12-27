package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.rad
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableRectangleBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class StaticBox private constructor(state: State) : RigidBody, Visible, Dynamic, Editable<StaticBox> {
    override val body = state.body
    override val physicsBody = Body(
        shape = Polygon(body.size.width / 2f, body.size.height / 2f),
        x = body.position.x,
        y = body.position.y,
    ).apply {
        density = 0f
        orientation = body.rotation
    }

    @set:Exposed(name = "isRotating")
    var isRotating = state.isRotating

    override fun update(deltaTimeInMillis: Float) {
        if (isRotating) {
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
        body = body,
        isRotating = isRotating,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableRectangleBody = RectangleBody(),
        @SerialName("isRotating") val isRotating: Boolean = false,
    ) : Serializable.State<StaticBox> {

        override fun restore() = StaticBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
