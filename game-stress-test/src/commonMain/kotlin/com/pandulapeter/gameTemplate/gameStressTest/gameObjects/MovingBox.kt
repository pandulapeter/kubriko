package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.editor.VisibleInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Movable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.deg
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import com.pandulapeter.gameTemplate.engine.implementation.extensions.trait
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableMapCoordinates
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableMapSize
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableRotationDegrees
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableScale
import com.pandulapeter.gameTemplate.engine.types.MapCoordinates
import com.pandulapeter.gameTemplate.engine.types.MapSize
import com.pandulapeter.gameTemplate.engine.types.Scale
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits.Destructible
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

class MovingBox private constructor(
    state: State,
) : GameObject<MovingBox>(
    { AvailableInEditor(createEditorInstance = { position -> MovingBox(state = State(position = position)) }) },
    { Movable(directionDegrees = state.directionDegrees, speed = state.speed, friction = state.friction) },
    { Dynamic(updater = ::update) },
    { Visible(boundingBox = state.boundingBox, position = state.position, scale = state.scale, rotationDegrees = state.rotationDegrees, drawer = ::draw) },
    { Destructible() },
) {
    @set:VisibleInEditor(typeId = "boxColor")
    var boxColor: Color = state.boxColor

    private var isGrowing = true
    private val visible by lazy { trait<Visible>() }
    private val destructible by lazy { trait<Destructible>() }
    private val movable by lazy { trait<Movable>() }

    private fun update(deltaTimeMillis: Float) {
        visible.depth = -visible.position.y - visible.pivotOffset.y
        visible.rotationDegrees += (0.1f * deltaTimeMillis).deg
        if (visible.scale.horizontal >= 1.6f) {
            isGrowing = false
        }
        if (visible.scale.vertical <= 0.5f) {
            isGrowing = true
        }
        if (isGrowing) {
            visible.scale = Scale(
                horizontal = visible.scale.horizontal + 0.001f * deltaTimeMillis,
                vertical = visible.scale.vertical + 0.001f * deltaTimeMillis,
            )
        } else {
            visible.scale = Scale(
                horizontal = visible.scale.horizontal - 0.001f * deltaTimeMillis,
                vertical = visible.scale.vertical - 0.001f * deltaTimeMillis,
            )
        }
        visible.position += MapCoordinates(
            x = cos(visible.rotationDegrees.toRadians()),
            y = -sin(visible.rotationDegrees.toRadians()),
        )
    }

    private fun draw(scope: DrawScope) = scope.drawRect(
        color = lerp(boxColor, Color.Black, destructible.destructionState),
        size = visible.boundingBox.rawSize,
    )

    @Serializable
    data class State(
        @SerialName("boundingBox") val boundingBox: SerializableMapSize = MapSize(100f, 100f),
        @SerialName("pivotOffset") val pivotOffset: SerializableMapCoordinates = boundingBox.center,
        @SerialName("position") val position: SerializableMapCoordinates = MapCoordinates.Zero,
        @SerialName("scale") val scale: SerializableScale = Scale.Unit,
        @SerialName("rotationDegrees") val rotationDegrees: SerializableRotationDegrees = 0f.deg,
        @SerialName("directionDegrees") val directionDegrees: SerializableRotationDegrees = 0f.deg,
        @SerialName("speed") val speed: Float = 0f,
        @SerialName("friction") val friction: Float = 0.015f,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
    ) : Serializer<MovingBox> {

        override val typeId = TYPE_ID

        override fun instantiate() = MovingBox(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getSerializer() = State(
        boundingBox = visible.boundingBox,
        pivotOffset = visible.pivotOffset,
        position = visible.position,
        rotationDegrees = visible.rotationDegrees,
        scale = visible.scale,
        directionDegrees = movable.directionDegrees,
        speed = movable.speed,
        friction = movable.friction,
        boxColor = boxColor,
    )

    companion object {
        const val TYPE_ID = "dynamicBox"
    }
}
