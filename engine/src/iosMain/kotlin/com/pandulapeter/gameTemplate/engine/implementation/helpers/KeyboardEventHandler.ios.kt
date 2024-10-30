package com.pandulapeter.gameTemplate.engine.implementation.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIApplication
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit
): KeyboardEventHandler = object : KeyboardEventHandler {

    private val inputView = object : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {

        override fun canBecomeFirstResponder() = true

        override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
            super.pressesBegan(presses, withEvent)
            presses.filterIsInstance<UIPress>().forEach { press ->
                press.key?.keyCode?.let { onKeyPressed(Key(it)) }
            }
        }

        override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
            super.pressesEnded(presses, withEvent)
            presses.filterIsInstance<UIPress>().forEach { press ->
                press.key?.keyCode?.let { onKeyReleased(Key(it)) }
            }
        }

        override fun pressesCancelled(presses: Set<*>, withEvent: UIPressesEvent?) {
            super.pressesCancelled(presses, withEvent)
            presses.filterIsInstance<UIPress>().forEach { press ->
                press.key?.keyCode?.let { onKeyReleased(Key(it)) }
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