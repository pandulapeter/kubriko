package com.pandulapeter.gameTemplate.gamePong.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.editor.VisibleInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.deg
import com.pandulapeter.gameTemplate.engine.implementation.extensions.trait
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableMapCoordinates
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableMapSize
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableRotationDegrees
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableScale
import com.pandulapeter.gameTemplate.engine.types.MapCoordinates
import com.pandulapeter.gameTemplate.engine.types.MapSize
import com.pandulapeter.gameTemplate.engine.types.Scale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Box private constructor(
    state: State,
) : GameObject<Box>(
    { AvailableInEditor(createEditorInstance = { position -> Box(state = State(position = position)) }) },
    { Visible(boundingBox = state.boundingBox, position = state.position, drawer = ::draw) },
) {
    @set:VisibleInEditor(typeId = "boxColor")
    var boxColor: Color = state.boxColor

    private val visible = trait<Visible>()

    fun draw(scope: DrawScope) = scope.drawRect(
        color = boxColor,
        size = visible.boundingBox.rawSize,
    )

    @Serializable
    data class State(
        @SerialName("boundingBox") val boundingBox: SerializableMapSize = MapSize(100f, 100f),
        @SerialName("pivotOffset") val pivotOffset: SerializableMapCoordinates = boundingBox.center,
        @SerialName("position") val position: SerializableMapCoordinates = MapCoordinates.Zero,
        @SerialName("scale") val scale: SerializableScale = Scale.Unit,
        @SerialName("rotationDegrees") val rotationDegrees: SerializableRotationDegrees = 0f.deg,
        @SerialName("depth") val depth: Float = 0f,
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
    ) : Serializer<Box> {

        override val typeId = TYPE_ID

        override fun instantiate() = Box(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getSerializer() = State(
        boundingBox = visible.boundingBox,
        pivotOffset = visible.pivotOffset,
        position = visible.position,
        scale = visible.scale,
        rotationDegrees = visible.rotationDegrees,
        depth = visible.depth,
        boxColor = boxColor,
    )

    companion object {
        const val TYPE_ID = "box"
    }
}
