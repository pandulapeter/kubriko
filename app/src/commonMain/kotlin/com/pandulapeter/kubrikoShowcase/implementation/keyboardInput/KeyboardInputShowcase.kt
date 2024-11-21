package com.pandulapeter.kubrikoShowcase.implementation.keyboardInput

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager

@Composable
fun KeyboardInputShowcase(
    modifier: Modifier = Modifier,
) {
    val keyboardInputShowcaseManager = remember { KeyboardInputShowcaseManager() }
    KubrikoCanvas(
        kubriko = Kubriko.newInstance(
            KeyboardInputManager.newInstance(),
            keyboardInputShowcaseManager,
        ),
    )
    Keyboard(
        modifier = modifier,
        activeKeys = keyboardInputShowcaseManager.activeKeys.collectAsState().value,
    )
}