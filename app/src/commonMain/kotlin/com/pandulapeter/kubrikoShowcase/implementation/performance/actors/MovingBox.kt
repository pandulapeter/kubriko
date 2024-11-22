package com.pandulapeter.kubrikoShowcase.implementation.performance.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableAngleRadians
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableScale
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneOffset
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableScenePixel
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoShowcase.implementation.performance.actors.traits.Destructible
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

class MovingBox private constructor(state: State) : Editable<MovingBox>, Destructible, Visible {

    @set:Exposed(name = "edgeSize")
    var edgeSize: ScenePixel = state.edgeSize
        set(value) {
            field = value
            boundingBox = SceneSize(
                width = value,
                height = value
            )
        }

    @set:Exposed(name = "position")
    override var position: SceneOffset = state.position

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor

    @set:Exposed(name = "rotation")
    override var rotation: AngleRadians = state.rotation

    @set:Exposed(name = "scale")
    override var scale: Scale = state.scale

    override var drawingOrder = 0f
    override var boundingBox = SceneSize(
        width = state.edgeSize,
        height = state.edgeSize
    )
    override var destructionState = 0f
    override var direction = AngleRadians.Zero
    override var speed = ScenePixel.Zero
    private var isGrowing = true
    private var isMoving = true

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -position.y.raw - pivotOffset.y.raw
        rotation += (0.001f * deltaTimeInMillis * (1f - destructionState)).rad
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
            position += SceneOffset(
                x = cos(rotation.normalized).scenePixel,
                y = -sin(rotation.normalized).scenePixel,
            )
        }
    }

    override fun DrawScope.draw() = drawRect(
        color = lerp(boxColor, Color.Black, destructionState),
        size = boundingBox.raw,
    )

    override fun destroy(character: Visible) {
        super.destroy(character)
        isMoving = false
    }

    override fun save() = State(
        edgeSize = edgeSize,
        position = position,
        boxColor = boxColor,
        rotation = rotation,
        scale = scale,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("edgeSize") val edgeSize: SerializableScenePixel = 100f.scenePixel,
        @SerialName("position") val position: SerializableSceneOffset = SceneOffset.Zero,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("rotation") val rotation: SerializableAngleRadians = 0f.rad,
        @SerialName("scale") val scale: SerializableScale = Scale.Unit,
    ) : Serializable.State<MovingBox> {

        override fun restore() = MovingBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
