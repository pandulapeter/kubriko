package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Movable
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.cos
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.sin
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableAngleRadians
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableScale
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneOffset
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MovingBox private constructor(state: State) : Visible, Movable, Editable<MovingBox> {

    @set:Exposed(name = "edgeSize")
    var edgeSize: SceneUnit = state.edgeSize
        set(value) {
            field = value
            body.size = SceneSize(value, value)
        }

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor

    override var drawingOrder = 0f
    override var direction = AngleRadians.Zero
    override var speed = SceneUnit.Zero
    private var isGrowing = true

    override val body = RectangleBody(
        initialSize = SceneSize(edgeSize, edgeSize),
        initialPosition = state.position,
        initialRotation = state.rotation,
        initialScale = state.scale,
    )

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -body.position.y.raw - body.pivot.y.raw
        body.rotation += 0.001f.rad * deltaTimeInMillis
        if (body.scale.horizontal >= 1.6f) {
            isGrowing = false
        }
        if (body.scale.vertical <= 0.5f) {
            isGrowing = true
        }
        if (isGrowing) {
            body.scale = Scale(
                horizontal = body.scale.horizontal + 0.001f * deltaTimeInMillis,
                vertical = body.scale.vertical + 0.001f * deltaTimeInMillis,
            )
        } else {
            body.scale = Scale(
                horizontal = body.scale.horizontal - 0.001f * deltaTimeInMillis,
                vertical = body.scale.vertical - 0.001f * deltaTimeInMillis,
            )
        }
        body.position += SceneOffset(
            x = body.rotation.cos.sceneUnit,
            y = -body.rotation.sin.sceneUnit,
        )
    }

    override fun DrawScope.draw() = drawRect(
        color = boxColor,
        size = body.size.raw,
    )

    override fun save() = State(
        edgeSize = edgeSize,
        position = body.position,
        boxColor = boxColor,
        rotation = body.rotation,
        scale = body.scale,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("edgeSize") val edgeSize: SerializableSceneUnit = 100f.sceneUnit,
        @SerialName("position") val position: SerializableSceneOffset = SceneOffset.Zero,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("rotation") val rotation: SerializableAngleRadians = 0f.rad,
        @SerialName("scale") val scale: SerializableScale = Scale.Unit,
    ) : Serializable.State<MovingBox> {

        override fun restore() = MovingBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
