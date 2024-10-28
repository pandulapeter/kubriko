package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StaticBox private constructor(
    stateHolder: StateHolder,
) : Box<StaticBox>(
    color = stateHolder.color,
    edgeSize = stateHolder.edgeSize,
    position = stateHolder.position,
    rotationDegrees = stateHolder.rotationDegrees,
) {

    @Serializable
    data class StateHolder(
        @SerialName("color") val color: SerializableColor = Color.Gray,
        @SerialName("edgeSize") val edgeSize: Float = 100f,
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
        @SerialName("rotationDegrees") val rotationDegrees: Float = 0f,
    ) : State<StaticBox> {

        override val typeId = TYPE_ID

        override fun instantiate() = StaticBox(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getState() = StateHolder(
        color = color,
        edgeSize = bounds.width,
        position = position,
        rotationDegrees = rotationDegrees,
    )

    companion object {
        const val TYPE_ID = "staticBox"
    }
}
