package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Movable
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.cos
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.sin
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableRectangleBody
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MovingBox private constructor(state: State) : Visible, Movable, Editable<MovingBox> {
    override val body = state.body

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor

    override var drawingOrder = 0f
    override var direction = AngleRadians.Zero
    override var speed = SceneUnit.Zero
    private var isGrowing = true

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -body.position.y.raw - body.pivot.y.raw
        body.rotation += 0.001f.rad * deltaTimeInMillis
        if (body.scale.horizontal >= 1.6f) {
            isGrowing = false
        }
        if (body.scale.vertical <= 0.5f) {
            isGrowing = true
        }
        if (isGrowing) {
            body.scale = Scale(
                horizontal = body.scale.horizontal + 0.001f * deltaTimeInMillis,
                vertical = body.scale.vertical + 0.001f * deltaTimeInMillis,
            )
        } else {
            body.scale = Scale(
                horizontal = body.scale.horizontal - 0.001f * deltaTimeInMillis,
                vertical = body.scale.vertical - 0.001f * deltaTimeInMillis,
            )
        }
        body.position += SceneOffset(
            x = body.rotation.cos.sceneUnit,
            y = -body.rotation.sin.sceneUnit,
        )
    }

    override fun DrawScope.draw() = drawRect(
        color = boxColor,
        size = body.size.raw,
    )

    override fun save() = State(
        body = body,
        boxColor = boxColor,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableRectangleBody = RectangleBody(),
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
    ) : Serializable.State<MovingBox> {

        override fun restore() = MovingBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
