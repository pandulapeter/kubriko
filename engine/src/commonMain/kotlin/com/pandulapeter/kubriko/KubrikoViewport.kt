/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.implementation.InternalViewport

/**
 * This Composable should be embedded into applications to draw the game world and handle all related logic.
 *
 * @param modifier The [Modifier] to be applied to the viewport.
 * @param kubriko The [Kubriko] instance that will be used for the game within this Composable.
 * @param windowInsets The [WindowInsets] to be used for the viewport.
 */
@Composable
fun KubrikoViewport(
    modifier: Modifier = Modifier,
    kubriko: Kubriko,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) = InternalViewport(
    modifier = modifier,
    kubriko = kubriko,
    windowInsets = windowInsets,
)