package com.pandulapeter.kubriko.demoInput

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoInput.implementation.InputDemoStateHolder
import com.pandulapeter.kubriko.demoInput.implementation.InputDemoStateHolderImpl
import com.pandulapeter.kubriko.demoInput.implementation.ui.Keyboard

fun createInputDemoStateHolder(): InputDemoStateHolder = InputDemoStateHolderImpl()

@Composable
fun InputDemo(
    modifier: Modifier = Modifier,
    stateHolder: InputDemoStateHolder = createInputDemoStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as InputDemoStateHolderImpl
    DebugMenu(
        modifier = modifier,
        windowInsets = windowInsets,
        kubriko = stateHolder.kubriko,
        buttonAlignment = null,
    ) {
        KubrikoViewport(
            kubriko = stateHolder.kubriko,
        )
        Keyboard(
            modifier = modifier,
            activeKeys = stateHolder.inputDemoManager.activeKeys.collectAsState().value,
        )
    }
}