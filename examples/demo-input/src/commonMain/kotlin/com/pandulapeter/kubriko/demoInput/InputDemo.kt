package com.pandulapeter.kubriko.demoInput

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoInput.implementation.InputDemoManager
import com.pandulapeter.kubriko.demoInput.implementation.ui.Keyboard
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager

@Composable
fun InputDemo(
    modifier: Modifier = Modifier,
) {
    val inputDemoManager = remember { InputDemoManager() }
    KubrikoViewport(
        kubriko = Kubriko.newInstance(
            KeyboardInputManager.newInstance(),
            inputDemoManager,
        ),
    )
    Keyboard(
        modifier = modifier,
        activeKeys = inputDemoManager.activeKeys.collectAsState().value,
    )
}