package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
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
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.traits.Destructible
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

class MovingBox private constructor(state: State) : Visible, Destructible, Editable<MovingBox> {

    @set:Exposed(name = "edgeSize")
    var edgeSize: ScenePixel = state.edgeSize
        set(value) {
            field = value
            body.size = SceneSize(value, value)
        }

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor

    override var drawingOrder = 0f
    override var destructionState = 0f
    override var direction = AngleRadians.Zero
    override var speed = ScenePixel.Zero
    private var isGrowing = true
    private var isMoving = true


    override val body= RectangleBody(
        initialSize = SceneSize(edgeSize, edgeSize),
        initialPosition = state.position,
        initialRotation = state.rotation,
        initialScale = state.scale,
    )

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -body.position.y.raw - body.pivot.y.raw
        body.rotation += (0.001f * deltaTimeInMillis * (1f - destructionState)).rad
        if (body.scale.horizontal >= 1.6f) {
            isGrowing = false
        }
        if (body.scale.vertical <= 0.5f) {
            isGrowing = true
        }
        if (isGrowing) {
            body.scale = Scale(
                horizontal = body.scale.horizontal + 0.001f * deltaTimeInMillis * (1f - destructionState),
                vertical = body.scale.vertical + 0.001f * deltaTimeInMillis * (1f - destructionState),
            )
        } else {
            body.scale = Scale(
                horizontal = body.scale.horizontal - 0.001f * deltaTimeInMillis * (1f - destructionState),
                vertical = body.scale.vertical - 0.001f * deltaTimeInMillis * (1f - destructionState),
            )
        }
        if (isMoving) {
            body.position += SceneOffset(
                x = cos(body.rotation.normalized).scenePixel,
                y = -sin(body.rotation.normalized).scenePixel,
            )
        }
    }

    override fun DrawScope.draw() = drawRect(
        color = lerp(boxColor, Color.Black, destructionState),
        size = body.size.raw,
    )

    override fun destroy(character: Visible) {
        super.destroy(character)
        isMoving = false
    }

    override fun save() = State(
        edgeSize = edgeSize,
        position = body.position,
        boxColor = boxColor,
        rotation = body.rotation,
        scale = body.scale,
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
