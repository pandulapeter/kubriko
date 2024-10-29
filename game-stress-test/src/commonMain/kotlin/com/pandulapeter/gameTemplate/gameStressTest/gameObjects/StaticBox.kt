package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Movable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.trait
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableSize
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits.Destructible
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StaticBox private constructor(
    state: State,
) : GameObject<StaticBox>(
    { AvailableInEditor(createEditorInstance = { position -> StaticBox(state = State(position = position)) }) },
    { Colorful(color = state.color) },
    { Visible(bounds = state.bounds, position = state.position, scale = state.scale, rotationDegrees = state.rotationDegrees, drawer = ::draw) },
    { Movable(directionDegrees = state.directionDegrees, speed = state.speed, friction = state.friction) },
    { Dynamic(updater = ::update) },
    { Destructible() },
) {
    private val visible by lazy { trait<Visible>() }
    private val colorful by lazy { trait<Colorful>() }
    private val destructible by lazy { trait<Destructible>() }
    private val movable by lazy { trait<Movable>() }

    private fun update(deltaTimeMillis: Float) {
        visible.depth = -visible.position.y - visible.pivot.y
    }

    private fun draw(scope: DrawScope) = scope.drawRect(
        color = lerp(colorful.color, Color.Black, destructible.destructionState),
        size = visible.bounds,
    )

    @Serializable
    data class State(
        @SerialName("color") val color: SerializableColor = Color.Gray,
        @SerialName("bounds") val bounds: SerializableSize = Size(100f, 100f),
        @SerialName("pivot") val pivot: SerializableOffset = bounds.center,
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
        @SerialName("scale") val scale: SerializableSize = Size(1f, 1f),
        @SerialName("rotationDegrees") val rotationDegrees: Float = 0f,
        @SerialName("directionDegrees") val directionDegrees: Float = 0f,
        @SerialName("speed") val speed: Float = 0f,
        @SerialName("friction") val friction: Float = 0.015f,
    ) : Serializer<StaticBox> {

        override val typeId = TYPE_ID

        override fun instantiate() = StaticBox(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getSerializer() = State(
        color = colorful.color,
        bounds = visible.bounds,
        pivot = visible.pivot,
        position = visible.position,
        scale = visible.scale,
        rotationDegrees = visible.rotationDegrees,
        directionDegrees = movable.directionDegrees,
        speed = movable.speed,
    )

    companion object {
        const val TYPE_ID = "staticBox"
    }
}
