package com.pandulapeter.kubrikoStressTest.implementation.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.sceneSerializer.Editable
import com.pandulapeter.kubriko.sceneSerializer.integration.EditableProperty
import com.pandulapeter.kubriko.sceneSerializer.serializers.SerializableAngleRadians
import com.pandulapeter.kubriko.sceneSerializer.serializers.SerializableColor
import com.pandulapeter.kubriko.sceneSerializer.serializers.SerializableSceneOffset
import com.pandulapeter.kubriko.sceneSerializer.serializers.SerializableScenePixel
import com.pandulapeter.kubriko.traits.Visible
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.traits.Destructible
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BoxWithCircle private constructor(state: BoxWithCircleState) : Editable<BoxWithCircle>, Destructible, Visible {

    @set:EditableProperty(name = "edgeSize")
    var edgeSize: ScenePixel = state.edgeSize
        set(value) {
            field = value
            boundingBox = SceneSize(
                width = value,
                height = value
            )
        }

    @set:EditableProperty(name = "position")
    override var position: SceneOffset = state.position

    @set:EditableProperty(name = "boxColor")
    var boxColor: Color = state.boxColor

    @set:EditableProperty(name = "circleColor")
    var circleColor: Color = state.circleColor

    @set:EditableProperty(name = "circleRadius")
    var circleRadius: ScenePixel = state.circleRadius

    @set:EditableProperty(name = "rotation")
    override var rotation: AngleRadians = state.rotation

    override var drawingOrder = 0f
    override var boundingBox = SceneSize(
        width = state.edgeSize,
        height = state.edgeSize
    )
    override var destructionState = 0f
    override var direction = AngleRadians.Zero
    override var speed = ScenePixel.Zero

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        drawingOrder = -position.y.raw - pivotOffset.y.raw
    }

    override fun draw(scope: DrawScope) {
        scope.drawRect(
            color = lerp(boxColor, Color.Black, destructionState),
            size = boundingBox.raw,
        )
        scope.drawCircle(
            color = lerp(circleColor, Color.Black, destructionState),
            radius = circleRadius.raw,
            center = boundingBox.center.raw,
        )
    }

    override fun save() = BoxWithCircleState(
        edgeSize = edgeSize,
        position = position,
        boxColor = boxColor,
        circleColor = circleColor,
        circleRadius = circleRadius,
        rotation = rotation,
    )

    @Serializable
    data class BoxWithCircleState(
        @SerialName("edgeSize") val edgeSize: SerializableScenePixel = 100f.scenePixel,
        @SerialName("position") val position: SerializableSceneOffset = SceneOffset.Zero,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("circleColor") val circleColor: SerializableColor = Color.White,
        @SerialName("circleRadius") val circleRadius: SerializableScenePixel = edgeSize / 3f,
        @SerialName("rotation") val rotation: SerializableAngleRadians = 0f.rad,
    ) : Editable.State<BoxWithCircle> {

        override fun restore() = BoxWithCircle(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
