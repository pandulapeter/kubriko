/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.lang.reflect.Proxy
import javax.swing.JComponent

/**
 * The trackpad's pinches over the window, each as how much it magnifies (above 0 to zoom in, below 0 to zoom out), for
 * what zooms (the sprite editor's canvas). Compose for Desktop doesn't report pinches itself; on macOS they come from
 * `com.apple.eawt.event` ([ProvideTrackpadPinches]), and elsewhere there are none.
 */
internal val LocalTrackpadPinches = staticCompositionLocalOf<SharedFlow<Double>> { MutableSharedFlow() }

/**
 * Provides [LocalTrackpadPinches] to [content], from the pinches over [component] (the window's root pane).
 */
@Composable
internal fun ProvideTrackpadPinches(component: JComponent, content: @Composable () -> Unit) {
    val pinches = remember { MutableSharedFlow<Double>(extraBufferCapacity = PINCH_BUFFER) }
    DisposableEffect(component) {
        val stopListening = component.listenForPinches { pinches.tryEmit(it) }
        onDispose { stopListening?.invoke() }
    }
    CompositionLocalProvider(LocalTrackpadPinches provides pinches, content = content)
}

/**
 * Hears the trackpad's pinches over this component through `com.apple.eawt.event`, which exists only in a JDK built for
 * macOS and is not exported by `java.desktop`, so it is reached through reflection, as the full screen notifications are
 * (ExtendedTitleBar.kt), and the macOS build starts with the `--add-exports` that opens it.
 *
 * @return What unregisters the listener, or null where none was registered.
 */
private fun JComponent.listenForPinches(onPinch: (Double) -> Unit): (() -> Unit)? {
    if (!isMacOs) return null
    return try {
        val utilities = Class.forName("$EVENT_PACKAGE.GestureUtilities")
        val gestureListener = Class.forName("$EVENT_PACKAGE.GestureListener")
        val magnificationListener = Class.forName("$EVENT_PACKAGE.MagnificationListener")
        val listener = Proxy.newProxyInstance(magnificationListener.classLoader, arrayOf(magnificationListener)) { proxy, method, arguments ->
            when (method.name) {
                "magnify" -> (arguments[0].javaClass.getMethod("getMagnification").invoke(arguments[0]) as? Double)?.let(onPinch)
                "equals" -> return@newProxyInstance proxy === arguments[0]
                "hashCode" -> return@newProxyInstance System.identityHashCode(proxy)
                "toString" -> return@newProxyInstance "MagnificationListener"
            }
            null
        }
        val component = this
        utilities.getMethod("addGestureListenerTo", JComponent::class.java, gestureListener).invoke(null, component, listener)
        val removeListener = utilities.getMethod("removeGestureListenerFrom", JComponent::class.java, gestureListener)
        return { removeListener.invoke(null, component, listener) }
    } catch (exception: Exception) {
        println("Trackpad pinches are not available: $exception")
        null
    }
}

private const val EVENT_PACKAGE = "com.apple.eawt.event"
private const val PINCH_BUFFER = 64
