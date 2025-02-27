/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DebugMenu : DebugMenuContract {

    override val isVisible = MutableStateFlow(false).asStateFlow()

    override fun toggleVisibility() = Unit

    @Composable
    override operator fun invoke(
        modifier: Modifier,
        windowInsets: WindowInsets,
        kubriko: Kubriko?,
        isEnabled: Boolean,
        buttonAlignment: Alignment?,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit,
        kubrikoViewport: @Composable () -> Unit,
        verticalDebugMenuWidth: Dp,
        horizontalDebugMenuHeight: Dp,
    ) = Unit

    @Composable
    override fun Horizontal(
        modifier: Modifier,
        kubriko: Kubriko?,
        isEnabled: Boolean,
        windowInsets: WindowInsets,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit,
        height: Dp,
    ) = Unit

    @Composable
    override fun Vertical(
        modifier: Modifier,
        kubriko: Kubriko?,
        isEnabled: Boolean,
        windowInsets: WindowInsets,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit,
        width: Dp,
    ) = Unit

    @Composable
    override fun OverlayOnly(
        modifier: Modifier,
        kubriko: Kubriko?,
        kubrikoViewport: @Composable () -> Unit,
        buttonAlignment: Alignment?,
    ) = Unit
}