package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubrikoShowcase.implementation.performance.extensions.handleDragAndPan
import com.pandulapeter.kubrikoShowcase.implementation.performance.extensions.handleMouseZoom

@Composable
fun PerformanceShowcase(
    modifier: Modifier = Modifier,
) = Box(modifier = modifier) {
    val performanceShowcaseKubrikoWrapper = remember { PerformanceShowcaseKubrikoWrapper() }
    DebugMenu(
        contentModifier = modifier,
        kubriko = performanceShowcaseKubrikoWrapper.kubriko,
    ) {
        KubrikoViewport(
            modifier = Modifier
                .handleMouseZoom(
                    stateManager = performanceShowcaseKubrikoWrapper.performanceShowcaseManager.stateManager,
                    viewportManager = performanceShowcaseKubrikoWrapper.performanceShowcaseManager.viewportManager,
                )
                .handleDragAndPan(
                    stateManager = performanceShowcaseKubrikoWrapper.performanceShowcaseManager.stateManager,
                    viewportManager = performanceShowcaseKubrikoWrapper.performanceShowcaseManager.viewportManager,
                )
                .background(Color.Black),
            kubriko = performanceShowcaseKubrikoWrapper.kubriko,
        )
        PlatformSpecificContent()
    }
}