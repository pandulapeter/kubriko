package com.pandulapeter.gameTemplate.editor

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.editor.implementation.EditorUserInterface
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleDragAndPan
import com.pandulapeter.gameTemplate.engine.EngineCanvas

@Composable
fun EditorApp(
    modifier: Modifier = Modifier,
) {
    EngineCanvas(
        modifier = modifier
            .handleMouseZoom()
            .handleDragAndPan()
            .background(Color.White),
    )
    EditorUserInterface(modifier)
}