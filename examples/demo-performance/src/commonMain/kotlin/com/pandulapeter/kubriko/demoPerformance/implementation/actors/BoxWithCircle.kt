package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableAngleRadians
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneOffset
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableScenePixel
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.traits.Destructible
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BoxWithCircle private constructor(state: State) : Visible, Destructible, Editable<BoxWithCircle> {

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
    var position: SceneOffset = state.position

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor

    @set:Exposed(name = "circleColor")
    var circleColor: Color = state.circleColor

    @set:Exposed(name = "circleRadius")
    var circleRadius: ScenePixel = state.circleRadius

    @set:Exposed(name = "rotation")
    var rotation: AngleRadians = state.rotation

    override val layerIndex = 0
    override var drawingOrder = 0f
    var boundingBox = SceneSize(
        width = state.edgeSize,
        height = state.edgeSize
    )
    override var destructionState = 0f
    override var direction = AngleRadians.Zero
    override var speed = ScenePixel.Zero

    override val body= RectangleBody(
        initialSize = SceneSize(edgeSize, edgeSize),
        initialPosition = position,
        initialRotation = rotation
    )

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -position.y.raw - body.pivot.y.raw
    }

    override fun DrawScope.draw() {
        drawRect(
            color = lerp(boxColor, Color.Black, destructionState),
            size = boundingBox.raw,
        )
        drawCircle(
            color = lerp(circleColor, Color.Black, destructionState),
            radius = circleRadius.raw,
            center = boundingBox.center.raw,
        )
    }

    override fun save() = State(
        edgeSize = edgeSize,
        position = position,
        boxColor = boxColor,
        circleColor = circleColor,
        circleRadius = circleRadius,
        rotation = rotation,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("edgeSize") val edgeSize: SerializableScenePixel = 100f.scenePixel,
        @SerialName("position") val position: SerializableSceneOffset = SceneOffset.Zero,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("circleColor") val circleColor: SerializableColor = Color.White,
        @SerialName("circleRadius") val circleRadius: SerializableScenePixel = edgeSize / 3f,
        @SerialName("rotation") val rotation: SerializableAngleRadians = 0f.rad,
    ) : Serializable.State<BoxWithCircle> {

        override fun restore() = BoxWithCircle(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
