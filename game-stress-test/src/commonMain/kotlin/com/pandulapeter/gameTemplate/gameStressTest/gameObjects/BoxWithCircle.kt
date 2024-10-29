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

class BoxWithCircle private constructor(
    state: State,
) : GameObject<BoxWithCircle>(
    { AvailableInEditor(createEditorInstance = { position -> BoxWithCircle(state = State(position = position)) }) },
    { Visible(boundingBox = state.boundingBox, position = state.position, scale = state.scale, rotationDegrees = state.rotationDegrees, drawer = ::draw) },
    { Movable(directionDegrees = state.directionDegrees, speed = state.speed, friction = state.friction) },
    { Dynamic(updater = ::update) },
    { Destructible() },
) {
    @set:VisibleInEditor(typeId = "boxColor")
    var boxColor: Color = state.boxColor

    @set:VisibleInEditor(typeId = "circleColor")
    var circleColor: Color = state.circleColor

    @set:VisibleInEditor(typeId = "circleRadius")
    var circleRadius: Float = state.circleRadius

    private val visible by lazy { trait<Visible>() }
    private val destructible by lazy { trait<Destructible>() }
    private val movable by lazy { trait<Movable>() }

    private fun update(deltaTimeMillis: Float) {
        visible.depth = -visible.position.y - visible.pivotOffset.y
    }

    private fun draw(scope: DrawScope) {
        scope.drawRect(
            color = lerp(boxColor, Color.Black, destructible.destructionState),
            size = visible.boundingBox.rawSize,
        )
        scope.drawCircle(
            color = lerp(circleColor, Color.Black, destructible.destructionState),
            radius = circleRadius,
            center = visible.boundingBox.center.rawOffset,
        )
    }

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
        @SerialName("circleColor") val circleColor: SerializableColor = Color.White,
        @SerialName("circleRadius") val circleRadius: Float = (boundingBox.width + boundingBox.height) / 4f,
    ) : Serializer<BoxWithCircle> {

        override val typeId = TYPE_ID

        override fun instantiate() = BoxWithCircle(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getSerializer() = State(
        boundingBox = visible.boundingBox,
        pivotOffset = visible.pivotOffset,
        position = visible.position,
        scale = visible.scale,
        rotationDegrees = visible.rotationDegrees,
        directionDegrees = movable.directionDegrees,
        speed = movable.speed,
        friction = movable.friction,
        boxColor = boxColor,
        circleColor = circleColor,
        circleRadius = circleRadius,
    )

    companion object {
        const val TYPE_ID = "staticBox"
    }
}
