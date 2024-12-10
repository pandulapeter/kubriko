package com.pandulapeter.kubriko.sceneEditor.implementation.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.implementation.EditorController

@Composable
internal fun EditorOverlay(
    modifier: Modifier = Modifier,
    editorController: EditorController,
) {
    val overlayManager = remember { OverlayManager(editorController) }
    KubrikoViewport(
        modifier = modifier,
        kubriko = Kubriko.newInstance(
            ViewportManager.newInstance(aspectRatioMode = ViewportManager.AspectRatioMode.Dynamic),
            overlayManager,
        ),
    ) {
        AnimatedVisibility(
            visible = editorController.shouldShowLoadingIndicator.collectAsState().value,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).align(Alignment.BottomStart),
                    strokeWidth = 3.dp,
                )
            }
        }
    }
}