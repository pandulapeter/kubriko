package com.pandulapeter.gameTemplate.editor.implementation.userInterface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible

private const val HIGHLIGHT_SIZE = 4f

internal fun DrawScope.selectedGameObjectHighlight(visible: Visible) {
    (visible.scale * Engine.get().viewportManager.scaleFactor.value).let { scale ->
        drawRect(
            color = Color.Black,
            topLeft = Offset(-HIGHLIGHT_SIZE / scale.width, -HIGHLIGHT_SIZE / scale.height),
            size = Size(visible.bounds.width + HIGHLIGHT_SIZE * 2 / scale.width, visible.bounds.height + HIGHLIGHT_SIZE * 2 / scale.height),
        )
    }
}