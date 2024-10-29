package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.State
import com.pandulapeter.gameTemplate.engine.gameObject.editor.Editable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.implementation.extensions.deg
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableAngleDegrees
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableScale
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableWorldCoordinates
import com.pandulapeter.gameTemplate.engine.types.AngleDegrees
import com.pandulapeter.gameTemplate.engine.types.Scale
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import com.pandulapeter.gameTemplate.engine.types.WorldSize
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits.Destructible
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

class MovingBox private constructor(state: MovingBoxState) : GameObject<MovingBox>, AvailableInEditor, Destructible {

    @set:Editable(typeId = "edgeSize")
    var edgeSize: Float = state.edgeSize
        set(value) {
            field = value
            boundingBox = WorldSize(
                width = value,
                height = value
            )
        }

    @set:Editable(typeId = "position")
    override var position: WorldCoordinates = state.position

    @set:Editable(typeId = "boxColor")
    var boxColor: Color = state.boxColor

    @set:Editable(typeId = "rotation")
    override var rotation: AngleDegrees = state.rotation

    @set:Editable(typeId = "scale")
    override var scale: Scale = state.scale

    override var drawingOrder = 0f
    override var boundingBox = WorldSize(
        width = state.edgeSize,
        height = state.edgeSize
    )
    override var destructionState = 0f
    override var direction = 0f.deg
    override var speed = 0f
    override var isSelectedInEditor = false
    private var isGrowing = true

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -position.y - pivotOffset.y
        rotation += (0.1f * deltaTimeInMillis).deg
        if (scale.horizontal >= 1.6f) {
            isGrowing = false
        }
        if (scale.vertical <= 0.5f) {
            isGrowing = true
        }
        if (isGrowing) {
            scale = Scale(
                horizontal = scale.horizontal + 0.001f * deltaTimeInMillis,
                vertical = scale.vertical + 0.001f * deltaTimeInMillis,
            )
        } else {
            scale = Scale(
                horizontal = scale.horizontal - 0.001f * deltaTimeInMillis,
                vertical = scale.vertical - 0.001f * deltaTimeInMillis,
            )
        }
        position += WorldCoordinates(
            x = cos(rotation.toRadians()),
            y = -sin(rotation.toRadians()),
        )
    }

    override fun draw(scope: DrawScope) {
        super.draw(scope)
        scope.drawRect(
            color = lerp(boxColor, Color.Black, destructionState),
            size = boundingBox.rawSize,
        )
    }

    override fun saveState() = MovingBoxState(
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
    ) : State<MovingBox> {

        override val typeId = TYPE_ID

        override fun restore() = MovingBox(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        const val TYPE_ID = "movingBox"
    }
}
