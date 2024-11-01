package com.pandulapeter.kubrikoStressTest.implementation.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.kubriko.engine.editorIntegration.EditableProperty
import com.pandulapeter.kubriko.engine.implementation.extensions.deg
import com.pandulapeter.kubriko.engine.implementation.extensions.toRadians
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableAngleDegrees
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableColor
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableScale
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableWorldCoordinates
import com.pandulapeter.kubriko.engine.traits.Editable
import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.types.AngleDegrees
import com.pandulapeter.kubriko.engine.types.Scale
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import com.pandulapeter.kubriko.engine.types.WorldSize
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.traits.Destructible
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

class MovingBox private constructor(state: MovingBoxState) : Editable<MovingBox>, Destructible {

    @set:EditableProperty(name = "edgeSize")
    var edgeSize: Float = state.edgeSize
        set(value) {
            field = value
            boundingBox = WorldSize(
                width = value,
                height = value
            )
        }

    @set:EditableProperty(name = "position")
    override var position: WorldCoordinates = state.position

    @set:EditableProperty(name = "boxColor")
    var boxColor: Color = state.boxColor

    @set:EditableProperty(name = "rotation")
    override var rotation: AngleDegrees = state.rotation

    @set:EditableProperty(name = "scale")
    override var scale: Scale = state.scale

    override var drawingOrder = 0f
    override var boundingBox = WorldSize(
        width = state.edgeSize,
        height = state.edgeSize
    )
    override var destructionState = 0f
    override var direction = 0f.deg
    override var speed = 0f
    private var isGrowing = true
    private var isMoving = true

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -position.y - pivotOffset.y
        rotation += (0.1f * deltaTimeInMillis * (1f - destructionState)).deg
        if (scale.horizontal >= 1.6f) {
            isGrowing = false
        }
        if (scale.vertical <= 0.5f) {
            isGrowing = true
        }
        if (isGrowing) {
            scale = Scale(
                horizontal = scale.horizontal + 0.001f * deltaTimeInMillis * (1f - destructionState),
                vertical = scale.vertical + 0.001f * deltaTimeInMillis * (1f - destructionState),
            )
        } else {
            scale = Scale(
                horizontal = scale.horizontal - 0.001f * deltaTimeInMillis * (1f - destructionState),
                vertical = scale.vertical - 0.001f * deltaTimeInMillis * (1f - destructionState),
            )
        }
        if (isMoving) {
            position += WorldCoordinates(
                x = cos(rotation.toRadians()),
                y = -sin(rotation.toRadians()),
            )
        }
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = lerp(boxColor, Color.Black, destructionState),
        size = boundingBox.rawSize,
    )

    override fun destroy(character: Visible) {
        super.destroy(character)
        isMoving = false
    }

    override fun save() = MovingBoxState(
        edgeSize = edgeSize,
        position = position,
        boxColor = boxColor,
        rotation = rotation,
        scale = scale,
    )

    @Serializable
    data class MovingBoxState(
        @SerialName("edgeSize") val edgeSize: Float = 100f,
        @SerialName("position") val position: SerializableWorldCoordinates = WorldCoordinates.Zero,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("rotation") val rotation: SerializableAngleDegrees = 0f.deg,
        @SerialName("scale") val scale: SerializableScale = Scale.Unit,
    ) : Editable.State<MovingBox> {

        override fun restore() = MovingBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
