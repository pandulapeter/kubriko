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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import com.pandulapeter.kubriko.implementation.windowState
import kubriko.app.desktop.generated.resources.Res
import kubriko.app.desktop.generated.resources.ic_icon
import kubriko.app.desktop.generated.resources.kubriko_showcase
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.event.WindowStateListener

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ApplicationScope.KubrikoShowcaseWindow(
    fullscreenState: DesktopFullscreenState,
    undecorated: Boolean,
    resizable: Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    var titleBar by remember { mutableStateOf<ExtendedTitleBar?>(null) }
    SwingWindow(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = stringResource(Res.string.kubriko_showcase),
        decoration = if (undecorated) WindowDecoration.Undecorated() else WindowDecoration.SystemDefault,
        resizable = resizable,
        icon = painterResource(Res.drawable.ic_icon),
        init = { window ->
            if (!undecorated) titleBar = window.extendContentIntoTitleBar()
            window.minimumSize = Dimension(400, 400)
        },
    ) {
        DisposableEffect(Unit) {
            val listener = WindowStateListener { fullscreenState.onWindowStateChanged(windowState) }
            window.addWindowStateListener(listener)
            onDispose {
                window.removeWindowStateListener(listener)
            }
        }
        titleBar?.let { TitleBarAppearance(window = window, titleBar = it) }
        TitleBarInsets(
            titleBar = titleBar,
            isFullscreen = windowState.placement == WindowPlacement.Fullscreen,
        ) {
            KubrikoShowcase(
                isInFullscreenMode = fullscreenState.isInFullscreenMode,
                getIsInFullscreenMode = { fullscreenState.isInFullscreenMode },
                onFullscreenModeToggled = { fullscreenState.toggle(window, windowState, coroutineScope) },
                onBackgroundColorChanged = { color -> window.setUndrawnAreaColor(Color(color.toArgb())) },
            )
        }
    }
}

/**
 * What the window shows wherever the Showcase has not been drawn yet - the edge a fast resize uncovers before the next
 * frame fills it - which is white unless it is told otherwise. On macOS that is the window's own background, and the
 * native surface the content is rendered into, a heavyweight component that takes its color when it is created rather
 * than following its ancestors'. On Windows it is the opaque Swing panels between the two as well, which Swing repaints
 * the uncovered edge with before the next frame arrives, in the look and feel's panel gray.
 */
private fun ComposeWindow.setUndrawnAreaColor(color: Color) {
    background = color
    fun Component.paintUndrawnArea() {
        if (isWindows || !isLightweight) background = color
        (this as? Container)?.components?.forEach { it.paintUndrawnArea() }
    }
    rootPane.paintUndrawnArea()
}
