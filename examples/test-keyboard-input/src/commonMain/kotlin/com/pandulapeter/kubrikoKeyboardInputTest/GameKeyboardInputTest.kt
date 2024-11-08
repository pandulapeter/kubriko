package com.pandulapeter.kubrikoKeyboardInputTest

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubrikoKeyboardInputTest.implementation.KeyboardInputTestManager
import com.pandulapeter.kubrikoKeyboardInputTest.implementation.UserInterface

@Composable
fun GameKeyboardInputTest(
    modifier: Modifier = Modifier,
) = MaterialTheme {
    val keyboardInputTestManager = remember { KeyboardInputTestManager() }
    KubrikoCanvas(
        kubriko = Kubriko.newInstance(
            KeyboardInputManager.newInstance(),
            keyboardInputTestManager,
        ),
    )
    UserInterface(
        modifier = modifier,
        activeKeys = keyboardInputTestManager.activeKeys.collectAsState().value,
    )
}