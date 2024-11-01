package com.pandulapeter.kubrikoPong.implementation.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.engine.editorIntegration.EditableProperty
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableColor
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableWorldCoordinates
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableWorldSize
import com.pandulapeter.kubriko.engine.traits.Editable
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import com.pandulapeter.kubriko.engine.types.WorldSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Box private constructor(state: BoxState) : Editable<Box> {

    @set:EditableProperty(name = "boundingBox")
    override var boundingBox: WorldSize = state.boundingBox

    @set:EditableProperty(name = "position")
    override var position: WorldCoordinates = state.position

    @set:EditableProperty(name = "boxColor")
    var boxColor: Color = state.boxColor

    override var isSelectedInEditor = false

    override fun draw(scope: DrawScope) {
        super.draw(scope)
        scope.drawRect(
            color = boxColor,
            size = boundingBox.rawSize,
        )
    }

    override fun saveState() = BoxState(
        boundingBox = boundingBox,
        position = position,
        boxColor = boxColor,
    )

    @Serializable
    data class BoxState(
        @SerialName("boundingBox") val boundingBox: SerializableWorldSize = WorldSize(100f, 100f),
        @SerialName("position") val position: SerializableWorldCoordinates = WorldCoordinates.Zero,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
    ) : Editable.State<Box> {

        override val typeId = TYPE_ID

        override fun restore() = Box(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        const val TYPE_ID = "box"
    }
}
