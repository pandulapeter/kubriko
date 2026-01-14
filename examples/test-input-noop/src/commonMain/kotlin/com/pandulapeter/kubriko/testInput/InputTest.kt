/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testInput

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.testInput.implementation.InputTestStateHolder
import com.pandulapeter.kubriko.testInput.implementation.InputTestStateHolderImpl

fun createInputTestStateHolder(
    isLoggingEnabled: Boolean,
): InputTestStateHolder = InputTestStateHolderImpl()

@Composable
fun InputTest(
    modifier: Modifier = Modifier,
    stateHolder: InputTestStateHolder = createInputTestStateHolder(
        isLoggingEnabled = false,
    ),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) = Unit