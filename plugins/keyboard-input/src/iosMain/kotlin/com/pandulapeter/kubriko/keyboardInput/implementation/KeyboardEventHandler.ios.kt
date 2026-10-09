/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.CoroutineScope
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.GameController.GCControllerButtonInput
import platform.GameController.GCKeyboard
import platform.GameController.GCKeyboardDidConnectNotification
import platform.GameController.GCKeyboardDidDisconnectNotification
import platform.GameController.GCKeyboardInput
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
internal actual fun createKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit,
    coroutineScope: CoroutineScope,
): KeyboardEventHandler = object : KeyboardEventHandler {

    private val listener = KeyListener(onKeyPressed, onKeyReleased)

    override fun startListening() = runOnMainThread { KeyboardRuntime.register(listener) }

    override fun stopListening() = runOnMainThread { KeyboardRuntime.unregister(listener) }

    @Composable
    override fun isValid() = true
}

private class KeyListener(
    val onKeyPressed: (Key) -> Unit,
    val onKeyReleased: (Key) -> Unit,
)

private inline fun runOnMainThread(crossinline action: () -> Unit) {
    if (NSThread.currentThread.isMainThread) {
        action()
    } else {
        dispatch_async(dispatch_get_main_queue()) { action() }
    }
}

/**
 * The process-wide owner of the coalesced keyboard's single `keyChangedHandler`, fanning its events out to every
 * registered listener. Unlike a first-responder view, it hears hardware keys whatever Compose has focused, without
 * taking them away from Compose. It replaces any handler the app itself installs on the coalesced keyboard.
 *
 * Only ever touched on the main thread. Listeners live in a copy-on-write array, so a callback that unregisters
 * (by disposing a Kubriko instance) cannot disturb the dispatch in progress, and dispatching allocates nothing.
 */
private object KeyboardRuntime {

    private var listeners = emptyArray<KeyListener>()
    private var keyboardInput: GCKeyboardInput? = null
    private var connectObserver: NSObjectProtocol? = null
    private var disconnectObserver: NSObjectProtocol? = null
    private var heldKeyCodes = LongArray(8)
    private var heldKeyCount = 0
    private val keyChangedHandler = { _: GCKeyboardInput?, _: GCControllerButtonInput?, keyCode: Long, isPressed: Boolean ->
        if (isPressed) onPressed(keyCode) else onReleased(keyCode)
    }

    fun register(listener: KeyListener) {
        if (listeners.contains(listener)) return
        listeners += listener
        if (listeners.size == 1) {
            NSNotificationCenter.defaultCenter.let { notificationCenter ->
                connectObserver = notificationCenter.addObserverForName(
                    name = GCKeyboardDidConnectNotification,
                    `object` = null,
                    queue = NSOperationQueue.mainQueue,
                ) { installHandler() }
                disconnectObserver = notificationCenter.addObserverForName(
                    name = GCKeyboardDidDisconnectNotification,
                    `object` = null,
                    queue = NSOperationQueue.mainQueue,
                ) {
                    releaseHeldKeys()
                    installHandler()
                }
            }
            installHandler()
        }
    }

    fun unregister(listener: KeyListener) {
        if (!listeners.contains(listener)) return
        listeners = listeners.filter { it !== listener }.toTypedArray()
        if (listeners.isEmpty()) {
            NSNotificationCenter.defaultCenter.let { notificationCenter ->
                connectObserver?.let(notificationCenter::removeObserver)
                disconnectObserver?.let(notificationCenter::removeObserver)
            }
            connectObserver = null
            disconnectObserver = null
            keyboardInput?.keyChangedHandler = null
            keyboardInput = null
            heldKeyCount = 0
        }
    }

    private fun installHandler() {
        val newKeyboardInput = GCKeyboard.coalescedKeyboard?.keyboardInput
        if (newKeyboardInput != keyboardInput) {
            keyboardInput?.keyChangedHandler = null
            keyboardInput = newKeyboardInput
        }
        newKeyboardInput?.keyChangedHandler = keyChangedHandler
    }

    private fun onPressed(keyCode: Long) {
        if (indexOfHeldKey(keyCode) == -1) {
            if (heldKeyCount == heldKeyCodes.size) {
                heldKeyCodes = heldKeyCodes.copyOf(heldKeyCount * 2)
            }
            heldKeyCodes[heldKeyCount++] = keyCode
        }
        val key = Key(keyCode)
        val currentListeners = listeners
        for (listener in currentListeners) {
            listener.onKeyPressed(key)
        }
    }

    private fun onReleased(keyCode: Long) {
        val index = indexOfHeldKey(keyCode)
        if (index != -1) {
            heldKeyCodes[index] = heldKeyCodes[--heldKeyCount]
        }
        val key = Key(keyCode)
        val currentListeners = listeners
        for (listener in currentListeners) {
            listener.onKeyReleased(key)
        }
    }

    /** A disconnected keyboard sends no release for the keys still held on it. */
    private fun releaseHeldKeys() {
        while (heldKeyCount > 0) {
            onReleased(heldKeyCodes[heldKeyCount - 1])
        }
    }

    private fun indexOfHeldKey(keyCode: Long): Int {
        for (index in 0 until heldKeyCount) {
            if (heldKeyCodes[index] == keyCode) return index
        }
        return -1
    }
}
