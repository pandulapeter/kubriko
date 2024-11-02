package com.pandulapeter.kubrikoPong.implementation.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.sceneEditorIntegration.EditableProperty
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.implementation.serializers.SerializableColor
import com.pandulapeter.kubriko.implementation.serializers.SerializableSceneOffset
import com.pandulapeter.kubriko.implementation.serializers.SerializableSceneSize
import com.pandulapeter.kubriko.traits.Editable
import com.pandulapeter.kubriko.traits.Visible
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Box private constructor(state: BoxState) : Editable<Box>, Visible {

    @set:EditableProperty(name = "boundingBox")
    override var boundingBox: SceneSize = state.boundingBox

    @set:EditableProperty(name = "position")
    override var position: SceneOffset = state.position

    @set:EditableProperty(name = "boxColor")
    var boxColor: Color = state.boxColor

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = boxColor,
        size = boundingBox.raw,
    )

    override fun save() = BoxState(
        boundingBox = boundingBox,
        position = position,
        boxColor = boxColor,
    )

    @Serializable
    data class BoxState(
        @SerialName("boundingBox") val boundingBox: SerializableSceneSize = SceneSize(100f.scenePixel, 100f.scenePixel),
        @SerialName("position") val position: SerializableSceneOffset = SceneOffset.Zero,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
    ) : Editable.State<Box> {

        override fun restore() = Box(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
