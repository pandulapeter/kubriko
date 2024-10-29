package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.editor.VisibleInEditor
import com.pandulapeter.gameTemplate.engine.implementation.extensions.deg
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableMapCoordinates
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableMapSize
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableRotationDegrees
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableScale
import com.pandulapeter.gameTemplate.engine.types.MapCoordinates
import com.pandulapeter.gameTemplate.engine.types.MapSize
import com.pandulapeter.gameTemplate.engine.types.RotationDegrees
import com.pandulapeter.gameTemplate.engine.types.Scale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@VisibleInEditor(typeId = "visible")
class Visible(
    @set:VisibleInEditor(typeId = "boundingBox") var boundingBox: MapSize,
    @set:VisibleInEditor(typeId = "pivotOffset") var pivotOffset: MapCoordinates = boundingBox.center,
    @set:VisibleInEditor(typeId = "position") var position: MapCoordinates,
    @set:VisibleInEditor(typeId = "scale") var scale: Scale = Scale.Unit,
    @set:VisibleInEditor(typeId = "rotationDegrees") var rotationDegrees: RotationDegrees = 0f.deg,
    @set:VisibleInEditor(typeId = "depth") var depth: Float = 0f,
    drawer: ((DrawScope) -> Unit)? = null,
) : Trait<Visible>() {

    private constructor(state: State) : this(
        boundingBox = state.boundingBox,
        pivotOffset = state.pivotOffset,
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
        @SerialName("boundingBox") val boundingBox: SerializableMapSize = MapSize.Zero,
        @SerialName("pivotOffset") val pivotOffset: SerializableMapCoordinates = boundingBox.center,
        @SerialName("position") val position: SerializableMapCoordinates = MapCoordinates.Zero,
        @SerialName("scale") val scale: SerializableScale = Scale.Unit,
        @SerialName("rotationDegrees") val rotationDegrees: SerializableRotationDegrees = 0f.deg,
        @SerialName("depth") val depth: Float = 0f,
    ) : Serializer<Visible> {

        constructor(visible: Visible) : this(
            boundingBox = visible.boundingBox,
            pivotOffset = visible.pivotOffset,
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