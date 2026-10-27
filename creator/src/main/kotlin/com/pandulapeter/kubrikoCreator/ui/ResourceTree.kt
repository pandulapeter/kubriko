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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.resource_group_empty
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ResourceTree(
    modifier: Modifier,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    onSceneSelected: (ProjectResource) -> Unit,
) = LazyColumn(
    modifier = modifier,
) {
    ResourceGroup.entries.forEach { group ->
        item(key = group) {
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                text = stringResource(group.title),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        val groupResources = resources[group].orEmpty()
        if (groupResources.isEmpty()) {
            item(key = "$group-empty") {
                ResourceTreeItem(
                    text = stringResource(Res.string.resource_group_empty),
                    onClick = null,
                )
            }
        } else {
            items(groupResources, key = { it.relativePath }) { resource ->
                ResourceTreeItem(
                    text = resource.name,
                    onClick = if (group == ResourceGroup.SCENES) ({ onSceneSelected(resource) }) else null,
                )
            }
        }
    }
}

@Composable
private fun ResourceTreeItem(
    text: String,
    onClick: (() -> Unit)?,
) = Text(
    modifier = Modifier
        .fillMaxWidth()
        .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
        .padding(horizontal = 24.dp, vertical = 4.dp),
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    color = if (onClick == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)
