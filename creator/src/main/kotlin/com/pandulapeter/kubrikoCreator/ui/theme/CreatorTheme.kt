/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

/**
 * Kubriko's theme with the Creator's own [CreatorTypography] in place of the shared one.
 */
@Composable
internal fun CreatorTheme(
    content: @Composable () -> Unit,
) = KubrikoTheme {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = CreatorTypography(),
        content = content,
    )
}
