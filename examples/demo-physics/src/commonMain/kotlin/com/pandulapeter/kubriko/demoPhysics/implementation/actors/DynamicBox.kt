package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableRectangleBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class DynamicBox private constructor(state: State) : BaseDynamicObject(), Editable<DynamicBox> {

    override val body = state.body
    override val physicsBody = Body(
        shape = Polygon(
            halfWidth = body.size.width / 2,
            halfHeight = body.size.height / 2,
        ),
        x = body.position.x,
        y = body.position.y,
    ).apply {
        restitution = 0.5f
        orientation = body.rotation
    }

    override fun DrawScope.draw() {
        drawRect(
            color = Color.LightGray,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(),
        )
        drawLine(
            color = Color.Black,
            start = Offset.Zero,
            end = Offset(body.size.width.raw, body.size.height.raw),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Black,
            start = Offset(body.size.width.raw, 0f),
            end = Offset(0f, body.size.height.raw),
            strokeWidth = 2f,
        )
    }

    override fun save() = State(
        body = body,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableRectangleBody = RectangleBody(),
    ) : Serializable.State<DynamicBox> {

        override fun restore() = DynamicBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
