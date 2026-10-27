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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_open_project
import kubriko.creator.generated.resources.app_name
import kubriko.creator.generated.resources.choose_project_folder
import kubriko.creator.generated.resources.welcome_subtitle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.io.File

@Composable
internal fun WelcomeScreen(
    error: StringResource?,
    onOpenProject: (File) -> Unit,
) {
    val pickerTitle = stringResource(Res.string.choose_project_folder)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = { pickProjectFolder(pickerTitle)?.let(onOpenProject) },
        ) {
            Text(stringResource(Res.string.action_open_project))
        }
        if (error != null) {
            Text(
                text = stringResource(error),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
