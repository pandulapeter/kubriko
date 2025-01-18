/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIApplication
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
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

    @Composable
    override fun isValid() = true
}

private fun UIPress.toKey() = key?.keyCode()?.run { Key(this) }