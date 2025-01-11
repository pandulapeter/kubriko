package com.pandulapeter.kubriko.demoPerformance

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolderImpl

fun createPerformanceDemoStateHolder(): PerformanceDemoStateHolder = PerformanceDemoStateHolderImpl()

@Composable
fun PerformanceDemo(
    modifier: Modifier = Modifier,
    stateHolder: PerformanceDemoStateHolder = createPerformanceDemoStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as PerformanceDemoStateHolderImpl
    DebugMenu(
        modifier = modifier,
        windowInsets = windowInsets,
        kubriko = stateHolder.kubriko,
        buttonAlignment = null,
    ) {
        KubrikoViewport(
            kubriko = stateHolder.kubriko,
            windowInsets = windowInsets,
        )
    }
}