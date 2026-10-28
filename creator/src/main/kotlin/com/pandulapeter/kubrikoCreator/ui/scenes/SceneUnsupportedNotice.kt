/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close_scene
import kubriko.creator.generated.resources.ic_warning
import kubriko.creator.generated.resources.scene_unsupported
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Says why a Scene can't be edited: the project's Kubriko version isn't Creator's, so its Actors can't be drawn here.
 */
@Composable
internal fun SceneUnsupportedNotice(
    sceneName: String,
    projectKubrikoVersion: String,
    creatorKubrikoVersion: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier.padding(32.dp),
    contentAlignment = Alignment.Center,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            modifier = Modifier.size(40.dp),
            painter = painterResource(Res.drawable.ic_warning),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = sceneName,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            modifier = Modifier.widthIn(max = 420.dp),
            text = stringResource(Res.string.scene_unsupported, creatorKubrikoVersion, projectKubrikoVersion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onClose) {
            Text(stringResource(Res.string.action_close_scene))
        }
    }
}
