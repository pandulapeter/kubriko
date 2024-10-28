package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Movable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableSize
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits.Destroyable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StaticBox private constructor(
    state: SerializerHolder,
) : GameObject<StaticBox>() {

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
                    color = lerp(colorful.color, Color.Black, destroyable.destructionState),
                    size = bounds,
                )
            }
        )
    }
    private val movable: Movable by lazy {
        Movable(
            directionDegrees = state.directionDegrees,
            speed = state.speed,
            dynamic = dynamic,
            visible = visible,
        )
    }
    private val dynamic: Dynamic by lazy {
        Dynamic()
    }
    private val destroyable: Destroyable by lazy {
        Destroyable(
            dynamic = dynamic,
            visible = visible,
            movable = movable,
        )
    }
    override val traits = setOf(
        visible,
        colorful,
        destroyable,
        dynamic,
        movable,
    )

    @Serializable
    data class SerializerHolder(
        @SerialName("color") val color: SerializableColor = Color.Gray,
        @SerialName("bounds") val bounds: SerializableSize = Size(100f, 100f),
        @SerialName("pivot") val pivot: SerializableOffset = bounds.center,
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
        @SerialName("depth") val depth: Float = 0f,
        @SerialName("scale") val scale: SerializableSize = Size(1f, 1f),
        @SerialName("rotationDegrees") val rotationDegrees: Float = 0f,
        @SerialName("directionDegrees") val directionDegrees: Float = 0f,
        @SerialName("speed") val speed: Float = 0f,
    ) : Serializer<StaticBox> {

        override val typeId = TYPE_ID

        override fun instantiate() = StaticBox(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getState() = SerializerHolder(
        color = colorful.color,
        bounds = visible.bounds,
        pivot = visible.pivot,
        position = visible.position,
        depth = visible.depth,
        scale = visible.scale,
        rotationDegrees = visible.rotationDegrees,
        directionDegrees = movable.directionDegrees,
        speed = movable.speed,
    )

    companion object {
        const val TYPE_ID = "staticBox"
    }
}
