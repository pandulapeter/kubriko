package com.pandulapeter.kubriko.sceneEditor.implementation.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.sceneEditor.implementation.EditorController

@Composable
internal fun EditorOverlay(
    modifier: Modifier = Modifier,
    editorController: EditorController,
) {
    val overlayManager = remember { OverlayManager(editorController) }
    KubrikoViewport(
        modifier = modifier,
        kubriko = Kubriko.newInstance(overlayManager),
    )
}