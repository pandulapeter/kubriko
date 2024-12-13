package com.pandulapeter.kubriko.demoInput

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoInput.implementation.InputDemoManager
import com.pandulapeter.kubriko.demoInput.implementation.ui.Keyboard
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager

@Composable
fun InputDemo(
    modifier: Modifier = Modifier,
    stateHolder: InputDemoStateHolder = createInputDemoStateHolder(),
) {
    stateHolder as InputDemoStateHolderImpl
    Keyboard(
        modifier = modifier,
        activeKeys = stateHolder.inputDemoManager.activeKeys.collectAsState().value,
    )
    KubrikoViewport(
        modifier = modifier,
        kubriko = stateHolder.kubriko,
    )
}

sealed interface InputDemoStateHolder {
    fun dispose()
}

fun createInputDemoStateHolder(): InputDemoStateHolder = InputDemoStateHolderImpl()

internal class InputDemoStateHolderImpl : InputDemoStateHolder {
    val inputDemoManager = InputDemoManager()
    val kubriko = Kubriko.newInstance(
        PointerInputManager.newInstance(),
        KeyboardInputManager.newInstance(),
        inputDemoManager,
    )

    override fun dispose() = kubriko.dispose()
}