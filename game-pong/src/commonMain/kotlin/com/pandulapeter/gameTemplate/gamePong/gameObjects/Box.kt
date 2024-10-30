package com.pandulapeter.gameTemplate.gamePong.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.EditorState
import com.pandulapeter.gameTemplate.engine.gameObject.editor.Editable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableWorldCoordinates
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableWorldSize
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import com.pandulapeter.gameTemplate.engine.types.WorldSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Box private constructor(state: BoxState) : AvailableInEditor<Box>, Visible {

    @set:Editable(typeId = "boundingBox")
    override var boundingBox: WorldSize = state.boundingBox

    @set:Editable(typeId = "position")
    override var position: WorldCoordinates = state.position

    @set:Editable(typeId = "boxColor")
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
    ) : EditorState<Box> {

        override val typeId = TYPE_ID

        override fun restore() = Box(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        const val TYPE_ID = "box"
    }
}
