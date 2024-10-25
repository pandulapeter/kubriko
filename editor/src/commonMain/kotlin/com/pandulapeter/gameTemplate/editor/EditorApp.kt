package com.pandulapeter.gameTemplate.editor

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.editor.implementation.EditorUserInterface
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleTouchGestures
import com.pandulapeter.gameTemplate.engine.EngineCanvas

@Composable
fun EditorApp(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { EditorController.start() }
    EngineCanvas(
        modifier = modifier
            .handleMouseZoom()
            .handleTouchGestures()
            .background(Color.Gray),
    )
    EditorUserInterface(modifier)
}