/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * Resumes [AudioContext]s that the browser's autoplay policy created suspended, on the first user gesture after they
 * were registered.
 */
internal object WebAudioUnlocker {
    const val STATE_SUSPENDED = "suspended"
    private const val STATE_RUNNING = "running"
    private const val STATE_CLOSED = "closed"

    /** A touch `pointerdown` grants no user activation, only the `pointerup` / `touchend` that follows it does. */
    private val activationEvents = arrayOf("pointerdown", "pointerup", "keydown", "touchend")
    private val pendingContexts = mutableListOf<AudioContext>()
    private val gestureListener: (Event) -> Unit = { onGesture() }

    fun register(context: AudioContext) {
        if (pendingContexts.contains(context)) return
        if (pendingContexts.isEmpty()) {
            activationEvents.forEach { window.addEventListener(it, gestureListener, true) }
        }
        pendingContexts.add(context)
    }

    fun unregister(context: AudioContext) {
        if (pendingContexts.remove(context) && pendingContexts.isEmpty()) {
            removeListeners()
        }
    }

    private fun onGesture() {
        val iterator = pendingContexts.iterator()
        while (iterator.hasNext()) {
            val context = iterator.next()
            when (context.state) {
                STATE_SUSPENDED -> context.resumeIgnoringRejection()
                STATE_RUNNING, STATE_CLOSED -> iterator.remove()
            }
        }
        if (pendingContexts.isEmpty()) {
            removeListeners()
        }
    }

    /** The capture flag is part of a listener's identity, so it must match the one the listeners were added with. */
    private fun removeListeners() = activationEvents.forEach { window.removeEventListener(it, gestureListener, true) }
}

/** Closing a context rejects a pending `resume()`, which would otherwise surface as an uncaught promise rejection. */
internal fun AudioContext.resumeIgnoringRejection() {
    resume().catch { null }
}
