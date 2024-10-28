package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Movable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableSize
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits.Destroyable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

class DynamicBox private constructor(
    state: SerializerHolder,
) : GameObject<DynamicBox>() {

    private var isGrowing = true

    private val movable: Movable by lazy {
        Movable(
            directionDegrees = state.directionDegrees,
            speed = state.speed,
            friction = state.friction,
            dynamic = dynamic,
            visible = visible,
        )
    }
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
        movable,
        visible,
        colorful,
        dynamic,
        destroyable,
    )

    init {
        dynamic.registerUpdater { deltaTimeMillis ->
            visible.rotationDegrees += 0.1f * deltaTimeMillis
            while (visible.rotationDegrees > 360f) {
                visible.rotationDegrees -= 360f
            }
            if (visible.scale.width >= 1.6f) {
                isGrowing = false
            }
            if (visible.scale.width <= 0.5f) {
                isGrowing = true
            }
            if (isGrowing) {
                visible.scale = Size(
                    width = visible.scale.width + 0.001f * deltaTimeMillis,
                    height = visible.scale.height + 0.001f * deltaTimeMillis,
                )
            } else {
                visible.scale = Size(
                    width = visible.scale.width - 0.001f * deltaTimeMillis,
                    height = visible.scale.height - 0.001f * deltaTimeMillis,
                )
            }
            visible.position += Offset(
                x = cos(visible.rotationDegrees.toRadians()),
                y = -sin(visible.rotationDegrees.toRadians()),
            )
        }
    }

    @Serializable
    data class SerializerHolder(
        @SerialName("color") val color: SerializableColor = Color.Gray,
        @SerialName("bounds") val bounds: SerializableSize = Size(100f, 100f),
        @SerialName("pivot") val pivot: SerializableOffset = bounds.center,
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
        @SerialName("scale") val scale: SerializableSize = Size(1f, 1f),
        @SerialName("rotationDegrees") val rotationDegrees: Float = 0f,
        @SerialName("depth") val depth: Float = 0f,
        @SerialName("directionDegrees") val directionDegrees: Float = 0f,
        @SerialName("speed") val speed: Float = 0f,
        @SerialName("friction") val friction: Float = 0.015f,
    ) : Serializer<DynamicBox> {

        override val typeId = TYPE_ID

        override fun instantiate() = DynamicBox(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getSerializer() = SerializerHolder(
        color = colorful.color,
        bounds = visible.bounds,
        pivot = visible.pivot,
        position = visible.position,
        rotationDegrees = visible.rotationDegrees,
        scale = visible.scale,
        depth = visible.depth,
        directionDegrees = movable.directionDegrees,
        speed = movable.speed,
    )

    companion object {
        const val TYPE_ID = "dynamicBox"
    }
}
