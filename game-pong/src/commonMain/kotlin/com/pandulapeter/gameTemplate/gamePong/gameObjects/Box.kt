package com.pandulapeter.gameTemplate.gamePong.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Box private constructor(
    state: SerializerHolder,
) : GameObject<Box>() {

    private val colorful: Colorful by lazy {
        Colorful(
            color = state.color,
        )
    }
    private val visible: Visible by lazy {
        Visible(
            bounds = state.bounds,
            position = state.position,
            scale = state.scale,
            rotationDegrees = state.rotationDegrees,
            depth = -state.position.y,
            draw = { scope ->
                scope.drawRect(
                    color = colorful.color,
                    size = bounds,
                )
            }
        )
    }
    override val traits = setOf(
        visible,
        colorful,
    )

    @Serializable
    data class SerializerHolder(
        @SerialName("color") val color: SerializableColor = Color.Gray,
        @SerialName("bounds") val bounds: SerializableSize = Size(100f, 100f),
        @SerialName("pivot") val pivot: SerializableOffset = bounds.center,
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
        @SerialName("scale") val scale: SerializableSize = Size(1f, 1f),
        @SerialName("rotationDegrees") val rotationDegrees: Float = 1f,
        @SerialName("depth") val depth: Float = 0f,
    ) : Serializer<Box> {

        override val typeId = TYPE_ID

        override fun instantiate() = Box(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getState() = SerializerHolder(
        color = colorful.color,
        bounds = visible.bounds,
        pivot = visible.pivot,
        position = visible.position,
        scale = visible.scale,
        rotationDegrees = visible.rotationDegrees,
        depth = visible.depth,
    )

    companion object {
        const val TYPE_ID = "box"
    }
}
