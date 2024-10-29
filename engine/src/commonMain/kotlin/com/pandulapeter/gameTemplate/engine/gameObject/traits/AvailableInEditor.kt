package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.implementation.extensions.getTrait
import com.pandulapeter.gameTemplate.engine.types.MapCoordinates
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// TODO: Get rid of this, all GameObjects should be available in the editor
class AvailableInEditor(
    var isSelectedInEditor: Boolean = false,
    val createEditorInstance: (MapCoordinates) -> GameObject<*>? = { null },
) : Trait<AvailableInEditor>() {

    private constructor(state: State) : this(
        isSelectedInEditor = state.isSelectedInEditor,
        createEditorInstance = { null },
    )

    override fun initialize() {
        gameObject.getTrait<Visible>()?.let { visible ->
            visible.registerDrawer { scope ->
                if (isSelectedInEditor) {
                    (visible.scale * Engine.get().viewportManager.scaleFactor.value).let { scale ->
                        scope.drawRect(
                            color = Color.Black.copy(alpha = 0.5f),
                            topLeft = Offset(
                                x = -HIGHLIGHT_SIZE / scale.horizontal,
                                y = -HIGHLIGHT_SIZE / scale.vertical
                            ),
                            size = Size(
                                width = visible.boundingBox.width + HIGHLIGHT_SIZE * 2 / scale.horizontal,
                                height = visible.boundingBox.height + HIGHLIGHT_SIZE * 2 / scale.vertical,
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun getSerializer(): Serializer<AvailableInEditor> = State(
        availableInEditor = this,
    )

    @Serializable
    private data class State(
        @SerialName("isSelectedInEditor") val isSelectedInEditor: Boolean = false,
    ) : Serializer<AvailableInEditor> {

        constructor(availableInEditor: AvailableInEditor) : this(
            isSelectedInEditor = availableInEditor.isSelectedInEditor,
        )

        override val typeId = "availableInEditor"

        override fun instantiate() = AvailableInEditor(
            state = this,
        )

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        private const val HIGHLIGHT_SIZE = 4f
    }
}