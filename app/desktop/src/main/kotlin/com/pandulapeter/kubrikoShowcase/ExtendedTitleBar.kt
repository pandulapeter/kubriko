/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * What is left of the system's title bar once the window's content is laid out under it: a strip of [height] at the
 * top that the system still drags the window by, with its window buttons drawn over the app's own background.
 *
 * @param customTitleBar The JetBrains Runtime's title bar, which is where the Windows buttons' colors are set.
 * @param mouseListener Marks the mouse events over the strip as the title bar's, until removed.
 */
internal class ExtendedTitleBar(
    val height: Dp,
    val customTitleBar: WindowDecorations.CustomTitleBar,
    val mouseListener: AWTEventListener,
)

/**
 * Lays the window's content out under the system's title bar on macOS and Windows, keeping only the window buttons,
 * so the Showcase's own surface is the title bar. Must be called before the window is shown, since the JDK does not
 * lay the content out again when this changes on a window already on screen.
 *
 * @return The strip the content is laid out under, or null where the window keeps the system's title bar: on Linux,
 *   where the window manager draws it, and without the JetBrains Runtime.
 */
internal fun ComposeWindow.extendContentIntoTitleBar(): ExtendedTitleBar? = when {
    isMacOs -> extendContentIntoCustomTitleBar(height = MAC_TITLE_BAR_HEIGHT)
    isWindows -> extendContentIntoCustomTitleBar(height = WINDOWS_TITLE_BAR_HEIGHT)
    else -> null
}

/**
 * The JetBrains Runtime's custom title bar gives the strip the title bar's behavior (dragging, snapping, the system
 * menu, and a double click that does what the system is set to do with one, which on macOS is Desktop & Dock's zoom,
 * fill, minimize or nothing) only where nothing listens to the mouse, and Compose's canvas listens everywhere. The
 * macOS transparent title bar client properties lay the content out the same way, but let the double click through to
 * the content, where it does nothing. A toolkit-wide listener runs after the runtime's own hit test and before it acts
 * on it, so it marks every mouse event over the strip as the title bar's. The Showcase keeps its content clear of the
 * strip ([TitleBarInsets]), except in full screen, where there is no strip.
 */
private fun ComposeWindow.extendContentIntoCustomTitleBar(height: Dp): ExtendedTitleBar? {
    val decorations = JBR.getWindowDecorations() ?: return null
    val titleBar = decorations.createCustomTitleBar().apply { this.height = height.value }
    decorations.setCustomTitleBar(this, titleBar)
    val mouseListener = AWTEventListener { event ->
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
    }
    Toolkit.getDefaultToolkit().addAWTEventListener(mouseListener, AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK)
    return ExtendedTitleBar(height = height, customTitleBar = titleBar, mouseListener = mouseListener)
}

/** The height of a macOS title bar without a toolbar, in points, which is what a dp is at the window's density. */
private val MAC_TITLE_BAR_HEIGHT = 28.dp

/** The height of a Windows 11 title bar and its caption buttons, in the scaled pixels a dp is at the window's density. */
private val WINDOWS_TITLE_BAR_HEIGHT = 32.dp
