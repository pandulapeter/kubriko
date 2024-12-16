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
import com.pandulapeter.kubriko.shared.ExampleStateHolder

@Composable
fun InputDemo(
    modifier: Modifier = Modifier,
    stateHolder: InputDemoStateHolder = createInputDemoStateHolder(),
) {
    stateHolder as InputDemoStateHolderImpl
    KubrikoViewport(
        kubriko = stateHolder.kubriko,
    )
    Keyboard(
        modifier = modifier,
        activeKeys = stateHolder.inputDemoManager.activeKeys.collectAsState().value,
    )
}

sealed interface InputDemoStateHolder : ExampleStateHolder

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