/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun InfoPanel(
    modifier: Modifier = Modifier,
    stringResource: StringResource,
    isVisible: Boolean,
) = AnimatedVisibility(
    visible = isVisible,
    enter = slideIn { IntOffset(0, -it.height) } + fadeIn(),
    exit = fadeOut() + slideOut { IntOffset(0, -it.height) },
) {
    Panel {
        Text(
            modifier = modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
            style = MaterialTheme.typography.bodySmall,
            text = stringResource(stringResource),
        )
    }
}