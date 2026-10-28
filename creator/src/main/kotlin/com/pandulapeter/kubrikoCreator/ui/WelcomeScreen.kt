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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.recent.RecentProject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_new_project
import kubriko.creator.generated.resources.action_open_project
import kubriko.creator.generated.resources.app_name
import kubriko.creator.generated.resources.choose_project_folder
import kubriko.creator.generated.resources.ic_create_new_folder
import kubriko.creator.generated.resources.ic_folder_open
import kubriko.creator.generated.resources.ic_icon
import kubriko.creator.generated.resources.welcome_subtitle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.io.File

@Composable
internal fun WelcomeScreen(
    error: StringResource?,
    recentProjects: List<RecentProject>,
    onNewProject: () -> Unit,
    onOpenProject: (File) -> Unit,
    onForgetRecentProject: (RecentProject) -> Unit,
) {
    val pickerTitle = stringResource(Res.string.choose_project_folder)
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        PrototypeBanner()
        WelcomeContent(
            modifier = Modifier.weight(1f),
            error = error,
            recentProjects = recentProjects,
            onNewProject = onNewProject,
            onOpenProject = onOpenProject,
            onPickFolder = { pickFolder(pickerTitle)?.let(onOpenProject) },
            onForgetRecentProject = onForgetRecentProject,
        )
    }
}

@Composable
private fun WelcomeContent(
    error: StringResource?,
    recentProjects: List<RecentProject>,
    onNewProject: () -> Unit,
    onOpenProject: (File) -> Unit,
    onPickFolder: () -> Unit,
    onForgetRecentProject: (RecentProject) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(96.dp),
            painter = painterResource(Res.drawable.ic_icon),
            contentDescription = null,
        )
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onNewProject) {
                ButtonLabel(Res.drawable.ic_create_new_folder, Res.string.action_new_project)
            }
            OutlinedButton(onClick = onPickFolder) {
                ButtonLabel(Res.drawable.ic_folder_open, Res.string.action_open_project)
            }
        }
        if (error != null) {
            Text(
                text = stringResource(error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (recentProjects.isNotEmpty()) {
            RecentProjectsList(
                modifier = Modifier.padding(top = 24.dp).widthIn(max = 520.dp),
                projects = recentProjects,
                onOpen = { onOpenProject(it.directory) },
                onForget = onForgetRecentProject,
            )
        }
    }
}

@Composable
private fun ButtonLabel(
    icon: DrawableResource,
    label: StringResource,
) {
    Icon(
        modifier = Modifier.padding(end = 8.dp).size(18.dp),
        painter = painterResource(icon),
        contentDescription = null,
    )
    Text(stringResource(label))
}
