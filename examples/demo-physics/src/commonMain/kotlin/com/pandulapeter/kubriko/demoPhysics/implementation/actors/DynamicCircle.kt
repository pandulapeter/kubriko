package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Circle
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableCircleBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class DynamicCircle private constructor(state: State) : BaseDynamicObject(), Editable<DynamicCircle> {
    override val body = state.body
    override val physicsBody = Body(
        shape = Circle(body.radius),
        x = body.position.x,
        y = body.position.y,
    ).apply {
        restitution = 0.5f
        orientation = body.rotation
    }

    override fun DrawScope.draw() {
        drawCircle(
            color = Color.LightGray,
            radius = body.radius.raw,
            center = body.size.center.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = body.radius.raw,
            center = body.size.center.raw,
            style = Stroke(),
        )
        drawLine(
            color = Color.Black,
            start = Offset(0f, body.radius.raw),
            end = Offset(body.size.width.raw, body.radius.raw),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Black,
            start = Offset(body.radius.raw, 0f),
            end = Offset(body.radius.raw, body.size.height.raw),
            strokeWidth = 2f,
        )
    }

    override fun save() = State(
        body = body,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableCircleBody = CircleBody(),
    ) : Serializable.State<DynamicCircle> {

        override fun restore() = DynamicCircle(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
