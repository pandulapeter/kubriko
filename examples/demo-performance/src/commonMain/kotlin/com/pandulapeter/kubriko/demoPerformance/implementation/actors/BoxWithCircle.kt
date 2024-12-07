package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.traits.Destructible
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableAngleRadians
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneOffset
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BoxWithCircle private constructor(state: State) : Visible, Destructible, Editable<BoxWithCircle> {

    @set:Exposed(name = "edgeSize")
    var edgeSize: SceneUnit = state.edgeSize
        set(value) {
            field = value
            body.size = SceneSize(value, value)
        }

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor

    @set:Exposed(name = "circleColor")
    var circleColor: Color = state.circleColor

    @set:Exposed(name = "circleRadius")
    var circleRadius: SceneUnit = state.circleRadius

    override val layerIndex = 0
    override var drawingOrder = 0f
    override var destructionState = 0f
    override var direction = AngleRadians.Zero
    override var speed = SceneUnit.Zero
    override val body = RectangleBody(
        initialSize = SceneSize(edgeSize, edgeSize),
        initialPosition = state.position,
        initialRotation = state.rotation
    )

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -body.position.y.raw - body.pivot.y.raw
    }

    override fun DrawScope.draw() {
        drawRect(
            color = lerp(boxColor, Color.Black, destructionState),
            size = body.size.raw,
        )
        drawCircle(
            color = lerp(circleColor, Color.Black, destructionState),
            radius = circleRadius.raw,
            center = body.size.center.raw,
        )
    }

    override fun save() = State(
        edgeSize = edgeSize,
        position = body.position,
        boxColor = boxColor,
        circleColor = circleColor,
        circleRadius = circleRadius,
        rotation = body.rotation,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("edgeSize") val edgeSize: SerializableSceneUnit = 100f.sceneUnit,
        @SerialName("position") val position: SerializableSceneOffset = SceneOffset.Zero,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("circleColor") val circleColor: SerializableColor = Color.White,
        @SerialName("circleRadius") val circleRadius: SerializableSceneUnit = edgeSize / 3f,
        @SerialName("rotation") val rotation: SerializableAngleRadians = 0f.rad,
    ) : Serializable.State<BoxWithCircle> {

        override fun restore() = BoxWithCircle(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
