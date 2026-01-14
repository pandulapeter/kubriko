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

interface DebugMenuContract {

    val isVisible: StateFlow<Boolean>

    fun toggleVisibility()

    // TODO: Remove this function once default arguments in member Composables become supported.
    // https://issuetracker.google.com/issues/165812010
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

    // TODO: Remove this function once default arguments in member Composables become supported.
    // https://issuetracker.google.com/issues/165812010
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

    @Composable
    fun Horizontal(
        modifier: Modifier, // TODO: = Modifier,
        kubriko: Kubriko?,
        isEnabled: Boolean, // TODO: = true,
        windowInsets: WindowInsets, // TODO: = WindowInsets.safeDrawing,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit, // TODO: = { it() },
        height: Dp, // TODO: = 180.dp,
    ) = Unit

    // TODO: Remove this function once default arguments in member Composables become supported.
    // https://issuetracker.google.com/issues/165812010
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

    @Composable
    fun Vertical(
        modifier: Modifier, // TODO: = Modifier,
        kubriko: Kubriko?,
        isEnabled: Boolean, // TODO: = true,
        windowInsets: WindowInsets, // TODO: = WindowInsets.safeDrawing,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit, // TODO: = { it() },
        width: Dp, // TODO: = 192.dp,
    ) = Unit

    // TODO: Remove this function once default arguments in member Composables become supported.
    // https://issuetracker.google.com/issues/165812010
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

    @Composable
    fun OverlayOnly(
        modifier: Modifier, // TODO: = Modifier,
        kubriko: Kubriko?,
        kubrikoViewport: @Composable () -> Unit,
        buttonAlignment: Alignment?, // TODO: = Alignment.TopStart,
    ) = Unit
}