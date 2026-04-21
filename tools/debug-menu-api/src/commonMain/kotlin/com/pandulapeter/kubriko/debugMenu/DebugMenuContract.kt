/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.flow.StateFlow

/**
 * Defines the public API for the Kubriko Debug Menu.
 *
 * The Debug Menu provides a way to inspect the internal state of the engine, view logs,
 * and manipulate various parameters at runtime.
 */
interface DebugMenuContract {

    /**
     * A flow indicating whether the debug menu is currently open.
     */
    val isVisible: StateFlow<Boolean>

    /**
     * Toggles the visibility of the debug menu.
     */
    fun toggleVisibility()

    /**
     * Renders the debug menu along with the game viewport.
     *
     * This is the recommended way to use the debug menu, as it handles the layout
     * and overlay button for you.
     *
     * @param kubriko The [Kubriko] instance to inspect.
     * @param isEnabled Whether the debug menu (and its toggle button) should be active.
     * @param kubrikoViewport A composable that renders the game scene.
     */
    @Composable
    operator fun invoke(
        kubriko: Kubriko?,
        isEnabled: Boolean,
        kubrikoViewport: @Composable () -> Unit,
    ) = invoke(
        modifier = Modifier,
        windowInsets = WindowInsets.safeDrawing,
        kubriko = kubriko,
        isEnabled = isEnabled,
        buttonAlignment = Alignment.TopStart,
        debugMenuTheme = { it() },
        kubrikoViewport = kubrikoViewport,
        verticalDebugMenuWidth = 192.dp,
        horizontalDebugMenuHeight = 160.dp,
    )

    /**
     * Detailed version of [invoke] for advanced customization.
     */
    @Composable
    operator fun invoke(
        modifier: Modifier, // TODO: = Modifier,
        windowInsets: WindowInsets, // TODO: = WindowInsets.safeDrawing,
        kubriko: Kubriko?,
        isEnabled: Boolean, // TODO: = true,
        buttonAlignment: Alignment?, // TODO: = Alignment.TopStart,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit, // TODO: = { it() },
        kubrikoViewport: @Composable () -> Unit,
        verticalDebugMenuWidth: Dp, // TODO: = 192.dp
        horizontalDebugMenuHeight: Dp, // TODO: = 160.dp
    ) = Unit

    /**
     * Renders only the horizontal version of the debug menu.
     */
    @Composable
    fun Horizontal(
        kubriko: Kubriko?,
        isEnabled: Boolean, // TODO: = true,
        windowInsets: WindowInsets, // TODO: = WindowInsets.safeDrawing,
    ) = Horizontal(
        modifier = Modifier,
        kubriko = kubriko,
        isEnabled = isEnabled,
        windowInsets = windowInsets,
        debugMenuTheme = { it() },
        height = 180.dp,
    )

    /**
     * Detailed version of [Horizontal] for advanced customization.
     */
    @Composable
    fun Horizontal(
        modifier: Modifier, // TODO: = Modifier,
        kubriko: Kubriko?,
        isEnabled: Boolean, // TODO: = true,
        windowInsets: WindowInsets, // TODO: = WindowInsets.safeDrawing,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit, // TODO: = { it() },
        height: Dp, // TODO: = 180.dp,
    ) = Unit

    /**
     * Renders only the vertical version of the debug menu.
     */
    @Composable
    fun Vertical(
        kubriko: Kubriko?,
        isEnabled: Boolean, // TODO: = true,
        windowInsets: WindowInsets, // TODO: = WindowInsets.safeDrawing,
    ) = Vertical(
        modifier = Modifier,
        kubriko = kubriko,
        isEnabled = isEnabled,
        windowInsets = windowInsets,
        debugMenuTheme = { it() },
        width = 192.dp,
    )

    /**
     * Detailed version of [Vertical] for advanced customization.
     */
    @Composable
    fun Vertical(
        modifier: Modifier, // TODO: = Modifier,
        kubriko: Kubriko?,
        isEnabled: Boolean, // TODO: = true,
        windowInsets: WindowInsets, // TODO: = WindowInsets.safeDrawing,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit, // TODO: = { it() },
        width: Dp, // TODO: = 192.dp,
    ) = Unit

    /**
     * Renders only the overlay button that toggles the menu.
     */
    @Composable
    fun OverlayOnly(
        kubriko: Kubriko?,
        kubrikoViewport: @Composable () -> Unit,
        buttonAlignment: Alignment?,
    ) = OverlayOnly(
        modifier = Modifier,
        kubriko = kubriko,
        kubrikoViewport = kubrikoViewport,
        buttonAlignment = buttonAlignment,
    )

    /**
     * Detailed version of [OverlayOnly] for advanced customization.
     */
    @Composable
    fun OverlayOnly(
        modifier: Modifier, // TODO: = Modifier,
        kubriko: Kubriko?,
        kubrikoViewport: @Composable () -> Unit,
        buttonAlignment: Alignment?, // TODO: = Alignment.TopStart,
    ) = Unit
}
