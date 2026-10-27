/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.recent.RecentProject
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import java.io.File
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_forget_recent_project
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_folder_open
import kubriko.creator.generated.resources.recent_project_missing
import kubriko.creator.generated.resources.recent_projects
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The projects opened recently, each opened with a click.
 */
@Composable
internal fun RecentProjectsList(
    projects: List<RecentProject>,
    onOpen: (RecentProject) -> Unit,
    onForget: (RecentProject) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
        text = stringResource(Res.string.recent_projects),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column {
            projects.forEach { project ->
                RecentProjectRow(
                    project = project,
                    onOpen = { onOpen(project) },
                    onForget = { onForget(project) },
                )
            }
        }
    }
}

/**
 * A recent project, which opens on a click unless its folder is gone (moved or deleted), which it then says.
 */
@Composable
private fun RecentProjectRow(
    project: RecentProject,
    onOpen: () -> Unit,
    onForget: () -> Unit,
) {
    val exists = remember(project.path) { File(project.path).isDirectory }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = exists, onClick = onOpen).padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(Res.drawable.ic_folder_open),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (exists) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (exists) project.path else stringResource(Res.string.recent_project_missing, project.path),
                style = MaterialTheme.typography.bodySmall,
                color = if (exists) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
        }
        IconActionButton(
            icon = Res.drawable.ic_close,
            label = Res.string.action_forget_recent_project,
            onClick = onForget,
        )
    }
}
