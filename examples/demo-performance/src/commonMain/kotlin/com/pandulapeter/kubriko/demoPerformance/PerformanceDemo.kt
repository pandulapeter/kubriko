package com.pandulapeter.kubriko.demoPerformance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoKubrikoWrapper
import com.pandulapeter.kubriko.demoPerformance.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.demoPerformance.implementation.extensions.handleDragAndPan
import com.pandulapeter.kubriko.demoPerformance.implementation.extensions.handleMouseZoom

@Composable
fun PerformanceDemo(
    modifier: Modifier = Modifier,
) = Box(modifier = modifier) {
    val performanceShowcaseKubrikoWrapper = remember { PerformanceDemoKubrikoWrapper() }
    DebugMenu(
        contentModifier = modifier,
        kubriko = performanceShowcaseKubrikoWrapper.kubriko,
    ) {
        KubrikoViewport(
            modifier = Modifier
                .handleMouseZoom(
                    stateManager = performanceShowcaseKubrikoWrapper.performanceDemoManager.stateManager,
                    viewportManager = performanceShowcaseKubrikoWrapper.performanceDemoManager.viewportManager,
                )
                .handleDragAndPan(
                    stateManager = performanceShowcaseKubrikoWrapper.performanceDemoManager.stateManager,
                    viewportManager = performanceShowcaseKubrikoWrapper.performanceDemoManager.viewportManager,
                )
                .background(Color.Black),
            kubriko = performanceShowcaseKubrikoWrapper.kubriko,
        )
        PlatformSpecificContent()
    }
}