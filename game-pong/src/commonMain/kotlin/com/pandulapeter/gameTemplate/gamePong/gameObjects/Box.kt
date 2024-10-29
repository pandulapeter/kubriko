package com.pandulapeter.gameTemplate.gamePong.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Colorful
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
    { Colorful(color = state.color) },
    { Visible(boundingBox = state.boundingBox, position = state.position, drawer = ::draw) },
) {
    private val colorful = trait<Colorful>()
    private val visible = trait<Visible>()

    fun draw(scope: DrawScope) = scope.drawRect(
        color = colorful.color,
        size = visible.boundingBox.rawSize,
    )

    @Serializable
    data class State(
        @SerialName("color") val color: SerializableColor = Color.Gray,
        @SerialName("boundingBox") val boundingBox: SerializableMapSize = MapSize(100f, 100f),
        @SerialName("pivotOffset") val pivotOffset: SerializableMapCoordinates = boundingBox.center,
        @SerialName("position") val position: SerializableMapCoordinates = MapCoordinates.Zero,
        @SerialName("scale") val scale: SerializableScale = Scale.Unit,
        @SerialName("rotationDegrees") val rotationDegrees: SerializableRotationDegrees = 0f.deg,
        @SerialName("depth") val depth: Float = 0f,
    ) : Serializer<Box> {

        override val typeId = TYPE_ID

        override fun instantiate() = Box(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getSerializer() = State(
        color = colorful.color,
        boundingBox = visible.boundingBox,
        pivotOffset = visible.pivotOffset,
        position = visible.position,
        scale = visible.scale,
        rotationDegrees = visible.rotationDegrees,
        depth = visible.depth,
    )

    companion object {
        const val TYPE_ID = "box"
    }
}
