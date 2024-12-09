package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
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
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class MovingBox private constructor(state: State) : Visible, Dynamic, Editable<MovingBox> {
    override val body = state.body

    @set:Exposed(name = "isRotatingClockwise")
    var isRotatingClockwise = state.isRotatingClockwise

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor

    private var isGrowing = true

    override fun onAdded(kubriko: Kubriko) {
        body.scale = ((5..16).random() / 10f).let { Scale(it,it) }
    }

    override fun update(deltaTimeInMillis: Float) {
        body.rotation += 0.001f.rad * deltaTimeInMillis * (if (isRotatingClockwise) 1 else -1)
        if (body.scale.horizontal >= 1.6f) {
            isGrowing = false
        } else if (body.scale.horizontal <= 0.5f) {
            isGrowing = true
        }
        if (isGrowing) {
            body.scale += deltaTimeInMillis * 0.001f
        } else {
            body.scale -= deltaTimeInMillis * 0.001f
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
        isRotatingClockwise = isRotatingClockwise,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableRectangleBody = RectangleBody(),
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("isRotatingClockwise") val isRotatingClockwise: Boolean = listOf(true, false).random(),
    ) : Serializable.State<MovingBox> {

        override fun restore() = MovingBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
