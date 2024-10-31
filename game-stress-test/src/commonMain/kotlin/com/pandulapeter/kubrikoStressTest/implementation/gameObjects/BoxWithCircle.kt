package com.pandulapeter.kubrikoStressTest.implementation.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.kubriko.engine.gameObject.editor.Editable
import com.pandulapeter.kubriko.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.kubriko.engine.implementation.extensions.deg
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableAngleDegrees
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableColor
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableWorldCoordinates
import com.pandulapeter.kubriko.engine.types.AngleDegrees
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import com.pandulapeter.kubriko.engine.types.WorldSize
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.traits.Destructible
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BoxWithCircle private constructor(state: BoxWithCircleState) : AvailableInEditor<BoxWithCircle>, Destructible {

    @set:Editable(name = "edgeSize")
    var edgeSize: Float = state.edgeSize
        set(value) {
            field = value
            boundingBox = WorldSize(
                width = value,
                height = value
            )
        }

    @set:Editable(name = "position")
    override var position: WorldCoordinates = state.position

    @set:Editable(name = "boxColor")
    var boxColor: Color = state.boxColor

    @set:Editable(name = "circleColor")
    var circleColor: Color = state.circleColor

    @set:Editable(name = "circleRadius")
    var circleRadius: Float = state.circleRadius

    @set:Editable(name = "rotation")
    override var rotation: AngleDegrees = state.rotation

    override var drawingOrder = 0f
    override var boundingBox = WorldSize(
        width = state.edgeSize,
        height = state.edgeSize
    )
    override var destructionState = 0f
    override var direction = 0f.deg
    override var speed = 0f
    override var isSelectedInEditor = false

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -position.y - pivotOffset.y
    }

    override fun draw(scope: DrawScope) {
        super.draw(scope)
        scope.drawRect(
            color = lerp(boxColor, Color.Black, destructionState),
            size = boundingBox.rawSize,
        )
        scope.drawCircle(
            color = lerp(circleColor, Color.Black, destructionState),
            radius = circleRadius,
            center = boundingBox.center.rawOffset,
        )
    }

    override fun saveState() = BoxWithCircleState(
        edgeSize = edgeSize,
        position = position,
        boxColor = boxColor,
        circleColor = circleColor,
        circleRadius = circleRadius,
        rotation = rotation,
    )

    @Serializable
    data class BoxWithCircleState(
        @SerialName("edgeSize") val edgeSize: Float = 100f,
        @SerialName("position") val position: SerializableWorldCoordinates = WorldCoordinates.Zero,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("circleColor") val circleColor: SerializableColor = Color.White,
        @SerialName("circleRadius") val circleRadius: Float = edgeSize / 3f,
        @SerialName("rotation") val rotation: SerializableAngleDegrees = 0f.deg,
    ) : AvailableInEditor.State<BoxWithCircle> {

        override val typeId = TYPE_ID

        override fun restore() = BoxWithCircle(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        const val TYPE_ID = "boxWithCircle"
    }
}
