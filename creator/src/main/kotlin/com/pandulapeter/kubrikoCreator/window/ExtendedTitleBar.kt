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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.MouseEvent
import java.lang.reflect.Proxy
import javax.swing.SwingUtilities

/**
 * What is left of the system's title bar once the window's content is laid out under it: a strip of [height] at the
 * top that the system still drags the window by, with its window buttons drawn over the app's own background.
 *
 * @param customTitleBar The JetBrains Runtime's title bar, which is where the Windows buttons' colors are set.
 */
internal class ExtendedTitleBar(
    val height: Dp,
    val customTitleBar: WindowDecorations.CustomTitleBar,
)

/**
 * The title bar the system draws for a Java window is its own light gray (or on Windows, the accent or the system's
 * theme) whatever the app looks like, so on macOS and Windows the window's content is laid out under it instead,
 * keeping only the window buttons: the app's own surface is then the title bar, in every theme and color. The title
 * is not drawn, since the app bar is where the screens say where the user is. Called before the window is shown,
 * since the JDK does not lay the content out again when this changes on a window already on screen.
 *
 * @return The strip the content is laid out under, or null where the window keeps the system's title bar: on Linux,
 *   where the window manager draws it, and on a runtime other than the JetBrains Runtime.
 */
internal fun ComposeWindow.extendContentIntoTitleBar(): ExtendedTitleBar? = when {
    isMacOs -> extendContentIntoCustomTitleBar(height = MAC_TITLE_BAR_HEIGHT)
    isWindows -> extendContentIntoCustomTitleBar(height = WINDOWS_TITLE_BAR_HEIGHT)
    else -> null
}

/**
 * The JetBrains Runtime's custom title bar, which draws the window buttons over the content and gives the rest of the
 * strip the title bar's behavior - dragging, snapping, the system menu, and a double click that does what the system
 * is set to do with one (on macOS, Desktop & Dock's zoom, fill, minimize or nothing) - but only where nothing in the
 * window listens to the mouse, and Compose's canvas listens everywhere. (The macOS client properties that make a title
 * bar transparent lay the content out the same way, but the double click reaches the content there, and the window
 * never zooms.) So every mouse event over the strip is marked as the title bar's, which the runtime asks for per
 * event: an event listener of the toolkit runs after it has made its own guess and before it acts on it. Nothing of
 * the app is ever under the strip, which the screens keep clear of ([TitleBarInsets]), so there is nothing there to
 * take the mouse away from - except in full screen, where there is no strip and the app bar reaches the top edge. The
 * listener lives as long as the process does, like the one window it serves.
 */
private fun ComposeWindow.extendContentIntoCustomTitleBar(height: Dp): ExtendedTitleBar? {
    val decorations = JBR.getWindowDecorations() ?: return null
    val titleBar = decorations.createCustomTitleBar().apply { this.height = height.value }
    decorations.setCustomTitleBar(this, titleBar)
    Toolkit.getDefaultToolkit().addAWTEventListener(
        { event ->
            if (
                event is MouseEvent && event.id != MouseEvent.MOUSE_EXITED && event.id != MouseEvent.MOUSE_WHEEL &&
                placement != WindowPlacement.Fullscreen
            ) {
                val component = event.component
                if (component != null && SwingUtilities.getWindowAncestor(component) === this) {
                    // The height is measured from the top of the client area, which is where the root pane starts.
                    if (SwingUtilities.convertPoint(component, event.point, rootPane).y < titleBar.height) {
                        titleBar.forceHitTest(false)
                    }
                }
            }
        },
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK,
    )
    return ExtendedTitleBar(height = height, customTitleBar = titleBar)
}

/**
 * Whether the window is in full screen, which [TitleBarInsets] drops the strip for.
 *
 * Compose reads its window's placement again only when the window is resized or AWT reports a change of its state, and
 * macOS full screen is neither of the second: from a zoomed window - which on a screen with a camera housing and a
 * hidden Dock is already the size of the full screen one - the window changes into full screen without a single
 * resize, and `WindowState.placement` stays `Maximized`, keeping the strip at the top of a window that has no title
 * bar. So on macOS the system's own full screen notifications decide, once the first of them has arrived; until then,
 * and elsewhere, the window state does.
 */
@Composable
internal fun rememberIsFullscreen(
    window: ComposeWindow,
    windowState: WindowState,
): Boolean {
    var isMacFullScreen by remember(window) { mutableStateOf<Boolean?>(null) }
    DisposableEffect(window) {
        val stopListening = window.listenForMacFullScreen { isFullScreen -> isMacFullScreen = isFullScreen }
        onDispose { stopListening?.invoke() }
    }
    return isMacFullScreen ?: (windowState.placement == WindowPlacement.Fullscreen)
}

/**
 * Hears the window entering and leaving macOS full screen through `com.apple.eawt`, which exists only in a JDK built for
 * macOS and is not exported by `java.desktop`, so it is reached through reflection and the macOS build starts with the
 * `--add-exports` that opens it. The change is reported
 * as the animation starts rather than once it is over, so that the content moves with the window instead of jumping
 * after it.
 *
 * @return What unregisters the listener, or null where none was registered.
 */
private fun Window.listenForMacFullScreen(onChanged: (Boolean) -> Unit): (() -> Unit)? {
    if (!isMacOs) return null
    return try {
        val utilities = Class.forName("$EAWT_PACKAGE.FullScreenUtilities")
        val listenerClass = Class.forName("$EAWT_PACKAGE.FullScreenListener")
        val listener = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { proxy, method, arguments ->
            when (method.name) {
                "windowEnteringFullScreen" -> onChanged(true)
                "windowExitingFullScreen" -> onChanged(false)
                // The JDK keeps its listeners in a list, which finds the one to remove by equals.
                "equals" -> return@newProxyInstance proxy === arguments[0]
                "hashCode" -> return@newProxyInstance System.identityHashCode(proxy)
                "toString" -> return@newProxyInstance "FullScreenListener"
            }
            null
        }
        val window = this
        utilities.getMethod("addFullScreenListenerTo", Window::class.java, listenerClass).invoke(null, window, listener)
        val removeListener = utilities.getMethod("removeFullScreenListenerFrom", Window::class.java, listenerClass)
        return { removeListener.invoke(null, window, listener) }
    } catch (exception: Exception) {
        println("Full screen notifications are not available: $exception")
        null
    }
}

internal val isMacOs = System.getProperty("os.name").orEmpty().lowercase().contains("mac")

internal val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")

/**
 * The height of a macOS title bar without a toolbar, in points, which is what a dp is at the window's own density.
 */
private val MAC_TITLE_BAR_HEIGHT = 28.dp

/**
 * The height of a Windows 11 title bar and its caption buttons, in the scaled pixels a dp is at the window's density.
 */
private val WINDOWS_TITLE_BAR_HEIGHT = 32.dp

private const val EAWT_PACKAGE = "com.apple.eawt"
