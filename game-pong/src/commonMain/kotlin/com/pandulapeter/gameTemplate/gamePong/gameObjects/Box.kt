package com.pandulapeter.gameTemplate.gamePong.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Box private constructor(
    state: State,
) : GameObject<Box>() {

    private val availableInEditor: AvailableInEditor = AvailableInEditor(
        createEditorInstance = { position ->
            Box(
                state = State(
                    position = position
                )
            )
        }
    )
    private val colorful: Colorful = Colorful(
        color = state.color,
    )
    private val visible: Visible = Visible(
        bounds = state.bounds,
        position = state.position,
        scale = state.scale,
        rotationDegrees = state.rotationDegrees,
        depth = state.depth,
        drawer = ::draw,
    )

    init {
        registerTraits(
            availableInEditor,
            visible,
            colorful,
        )
    }

    private fun draw(scope: DrawScope) = scope.drawRect(
        color = colorful.color,
        size = visible.bounds,
    )

    @Serializable
    data class State(
        @SerialName("color") val color: SerializableColor = Color.Gray,
        @SerialName("bounds") val bounds: SerializableSize = Size(100f, 100f),
        @SerialName("pivot") val pivot: SerializableOffset = bounds.center,
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
        @SerialName("scale") val scale: SerializableSize = Size(1f, 1f),
        @SerialName("rotationDegrees") val rotationDegrees: Float = 1f,
        @SerialName("depth") val depth: Float = 0f,
    ) : Serializer<Box> {

        override val typeId = TYPE_ID

        override fun instantiate() = Box(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getSerializer() = State(
        color = colorful.color,
        bounds = visible.bounds,
        pivot = visible.pivot,
        position = visible.position,
        scale = visible.scale,
        rotationDegrees = visible.rotationDegrees,
        depth = visible.depth,
    )

    companion object {
        const val TYPE_ID = "box"
    }
}
