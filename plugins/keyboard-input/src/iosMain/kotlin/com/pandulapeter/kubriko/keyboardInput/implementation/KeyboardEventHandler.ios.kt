package com.pandulapeter.kubriko.keyboardInput.implementation

import androidx.compose.ui.input.key.Key
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIApplication
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
internal actual fun createKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit,
    coroutineScope: CoroutineScope,
): KeyboardEventHandler = object : KeyboardEventHandler {

    private val inputView = object : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {

        override fun canBecomeFirstResponder() = true

        override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
            super.pressesBegan(presses, withEvent)
            presses.filterIsInstance<UIPress>().forEach { press ->
                press.toKey()?.let(onKeyPressed)
            }
        }

        override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
            super.pressesEnded(presses, withEvent)
            presses.filterIsInstance<UIPress>().forEach { press ->
                press.toKey()?.let(onKeyReleased)
            }
        }

        override fun pressesCancelled(presses: Set<*>, withEvent: UIPressesEvent?) {
            super.pressesCancelled(presses, withEvent)
            presses.filterIsInstance<UIPress>().forEach { press ->
                press.toKey()?.let(onKeyReleased)
            }
        }
    }

    init {
        inputView.becomeFirstResponder()
        val currentViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        currentViewController?.view?.addSubview(inputView)
    }

    override fun startListening() = Unit

    override fun stopListening() = inputView.removeFromSuperview()
}

private fun UIPress.toKey() = key?.keyCode()?.run { Key(this) }