package com.pandulapeter.kubriko.demoInput.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoInput.implementation.managers.InputDemoManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shared.StateHolder

sealed interface InputDemoStateHolder : StateHolder

internal class InputDemoStateHolderImpl : InputDemoStateHolder {
    private val pointerInputManager = PointerInputManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val keyboardInputManager = KeyboardInputManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val inputDemoManager = InputDemoManager()
    override val kubriko = Kubriko.newInstance(
        pointerInputManager,
        keyboardInputManager,
        inputDemoManager,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )

    override fun dispose() = kubriko.dispose()
}

private const val LOG_TAG = "Input"