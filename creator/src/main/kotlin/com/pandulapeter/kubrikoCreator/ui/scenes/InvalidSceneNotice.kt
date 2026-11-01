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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.scenes.SceneFileProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_open_in_system_editor
import kubriko.creator.generated.resources.ic_warning
import kubriko.creator.generated.resources.scene_invalid_state
import kubriko.creator.generated.resources.scene_invalid_unparsable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Says why the open Scene's file can't be read, which no change can be made to until it is fixed by hand, offering to
 * open it in the system's editor.
 */
@Composable
internal fun InvalidSceneNotice(
    problem: SceneFileProblem,
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.errorContainer,
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(Res.drawable.ic_warning),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = when (problem) {
                is SceneFileProblem.Unparsable -> stringResource(Res.string.scene_invalid_unparsable, problem.reason)
                is SceneFileProblem.StateNotAnObject -> stringResource(Res.string.scene_invalid_state, problem.actorId)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        TextButton(
            onClick = onOpenFile,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
        ) {
            Text(stringResource(Res.string.action_open_in_system_editor))
        }
    }
}
