package com.pandulapeter.gameTemplate.gamePong.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Rotatable
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Box(
    stateHolder: StateHolder,
) : GameObject<Box>(), Rotatable, Colorful {

    override var rotationDegrees = stateHolder.rotationDegrees
    override var color = stateHolder.color
    override var bounds = stateHolder.bounds
    override var pivot = bounds.center
    override var position = stateHolder.position
    override var depth = 0f

    @Serializable
    data class StateHolder(
        @SerialName("color") val color: SerializableColor = Color.Gray,
        @SerialName("bounds") val bounds: SerializableSize = Size(100f, 100f),
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
        @SerialName("rotationDegrees") val rotationDegrees: Float = 0f,
    ) : State<Box> {

        override val typeId = TYPE_ID

        override fun instantiate() = Box(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = color,
        size = bounds,
    )

    override fun getState() = StateHolder(
        color = color,
        bounds = bounds,
        position = position,
        rotationDegrees = rotationDegrees,
    )

    companion object {
        const val TYPE_ID = "box"
    }
}
