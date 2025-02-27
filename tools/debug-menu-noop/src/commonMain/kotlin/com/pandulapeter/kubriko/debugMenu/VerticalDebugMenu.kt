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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko

@Composable
fun VerticalDebugMenu(
    modifier: Modifier = Modifier,
    kubriko: Kubriko?,
    isEnabled: Boolean = true,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit = { },
    width: Dp = 192.dp,
) = Unit