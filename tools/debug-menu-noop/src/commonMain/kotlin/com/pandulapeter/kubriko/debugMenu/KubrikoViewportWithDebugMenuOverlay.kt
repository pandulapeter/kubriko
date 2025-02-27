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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko

@Composable
fun KubrikoViewportWithDebugMenuOverlay(
    modifier: Modifier = Modifier,
    kubriko: Kubriko?,
    kubrikoViewport: @Composable () -> Unit,
    buttonAlignment: Alignment? = Alignment.TopStart,
) = Unit