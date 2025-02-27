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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun InfoPanel(
    modifier: Modifier = Modifier,
    text: String,
    isVisible: Boolean,
) = Column {
    val fadeOutProgress by animateFloatAsState(if (isVisible) 1f else 0f)
    Panel(
        modifier = Modifier.graphicsLayer(
            alpha = fadeOutProgress,
            clip = true,
            scaleX = fadeOutProgress,
            transformOrigin = TransformOrigin(0.9f, -0.2f),
        ),
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.CenterVertically),
            exit = shrinkVertically(shrinkTowards = Alignment.CenterVertically) + fadeOut(),
        ) {
            Text(
                modifier = modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
                style = MaterialTheme.typography.bodySmall,
                text = text,
            )
        }
    }
    Spacer(
        modifier = Modifier.height(8.dp * fadeOutProgress).fillMaxWidth(),
    )
}

@Composable
fun InfoPanel(
    modifier: Modifier = Modifier,
    stringResource: StringResource,
    isVisible: Boolean,
) = InfoPanel(
    modifier = modifier,
    text = stringResource(stringResource),
    isVisible = isVisible,
)