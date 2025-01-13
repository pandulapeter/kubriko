package com.pandulapeter.kubriko.demoPerformance

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
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
    KubrikoViewport(
        modifier = modifier,
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
}