package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.editor.VisibleInEditor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@VisibleInEditor(typeId = "visible")
class Visible(
    @VisibleInEditor(typeId = "bounds") var bounds: SerializableSize,
    @VisibleInEditor(typeId = "pivot") var pivot: SerializableOffset = bounds.center,
    @VisibleInEditor(typeId = "position") var position: SerializableOffset,
    @VisibleInEditor(typeId = "scale") var scale: SerializableSize = Size(1f, 1f),
    @VisibleInEditor(typeId = "rotationDegrees") var rotationDegrees: Float = 0f,
    @VisibleInEditor(typeId = "depth") var depth: Float = 0f,
    drawer: ((DrawScope) -> Unit)? = null,
) : Trait<Visible>() {

    private constructor(state: State) : this(
        bounds = state.bounds,
        pivot = state.pivot,
        position = state.position,
        scale = state.scale,
        rotationDegrees = state.rotationDegrees,
        depth = state.depth,
    )

    private val allDrawers = mutableListOf<(DrawScope) -> Unit>().apply {
        drawer?.let(::add)
    }

    fun registerDrawer(drawer: (DrawScope) -> Unit) {
        allDrawers.add(drawer)
    }

    fun draw(scope: DrawScope) = allDrawers.forEach { it(scope) }

    override fun getSerializer(): Serializer<Visible> = State(
        visible = this,
    )

    @Serializable
    private data class State(
        @SerialName("bounds") val bounds: SerializableSize = Size.Zero,
        @SerialName("pivot") val pivot: SerializableOffset = bounds.center,
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
        @SerialName("scale") val scale: SerializableSize = Size(1f, 1f),
        @SerialName("rotationDegrees") val rotationDegrees: Float = 1f,
        @SerialName("depth") val depth: Float = 0f,
    ) : Serializer<Visible> {

        constructor(visible: Visible) : this(
            bounds = visible.bounds,
            pivot = visible.pivot,
            position = visible.position,
            scale = visible.scale,
            rotationDegrees = visible.rotationDegrees,
            depth = visible.depth,
        )

        override val typeId = "visible"

        override fun instantiate() = Visible(
            state = this,
        )

        override fun serialize() = Json.encodeToString(this)
    }
}