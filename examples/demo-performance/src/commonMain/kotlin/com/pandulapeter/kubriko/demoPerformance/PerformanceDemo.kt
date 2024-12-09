package com.pandulapeter.kubriko.demoPerformance

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
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoKubrikoWrapper
import com.pandulapeter.kubriko.demoPerformance.implementation.PlatformSpecificContent

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
            kubriko = performanceShowcaseKubrikoWrapper.kubriko,
        )
        PlatformSpecificContent()
        AnimatedVisibility(
            visible = performanceShowcaseKubrikoWrapper.performanceDemoManager.shouldShowLoadingIndicator.collectAsState().value,
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