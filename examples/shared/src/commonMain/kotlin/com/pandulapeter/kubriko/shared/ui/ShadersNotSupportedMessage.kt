/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kubriko.examples.shared.generated.resources.Res
import kubriko.examples.shared.generated.resources.shaders_not_supported
import org.jetbrains.compose.resources.stringResource

/**
 * The centred message the shader demos show in place of their content on platforms without shader support.
 */
@Composable
fun ShadersNotSupportedMessage(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets,
) = Box(
    modifier = modifier
        .fillMaxSize()
        .windowInsetsPadding(windowInsets)
        .padding(16.dp),
) {
    Text(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .align(Alignment.Center),
        textAlign = TextAlign.Center,
        text = stringResource(Res.string.shaders_not_supported),
    )
}
