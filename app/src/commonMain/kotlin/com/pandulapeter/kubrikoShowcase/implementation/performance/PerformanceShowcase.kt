package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubrikoShowcase.implementation.performance.extensions.handleDragAndPan
import com.pandulapeter.kubrikoShowcase.implementation.performance.extensions.handleMouseZoom

@Composable
fun PerformanceShowcase(
    modifier: Modifier = Modifier,
) {
    val performanceShowcaseKubrikoWrapper = remember { PerformanceShowcaseKubrikoWrapper() }
    DebugMenu(
        contentModifier = modifier,
        kubriko = performanceShowcaseKubrikoWrapper.kubriko,
    ) {
        KubrikoCanvas(
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