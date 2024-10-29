package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.annotation.CallSuper
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates

// TODO: Get rid of this, all GameObjects should be available in the editor
interface AvailableInEditor : Visible {

    var isSelectedInEditor: Boolean

    fun createEditorInstance(position: WorldCoordinates): GameObject<*>

    @CallSuper
    override fun draw(scope: DrawScope) {
        if (isSelectedInEditor) {
            (scale * Engine.get().viewportManager.scaleFactor.value).let { scale ->
                scope.drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(
                        x = -HIGHLIGHT_SIZE / scale.horizontal,
                        y = -HIGHLIGHT_SIZE / scale.vertical
                    ),
                    size = Size(
                        width = boundingBox.width + HIGHLIGHT_SIZE * 2 / scale.horizontal,
                        height = boundingBox.height + HIGHLIGHT_SIZE * 2 / scale.vertical,
                    ),
                )
            }
        }
    }

    companion object {
        private const val HIGHLIGHT_SIZE = 4f
    }
}