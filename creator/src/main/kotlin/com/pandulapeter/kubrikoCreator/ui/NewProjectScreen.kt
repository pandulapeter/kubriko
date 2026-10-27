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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.template.NewProjectForm
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectTemplate
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_browse
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_create_project
import kubriko.creator.generated.resources.choose_parent_folder
import kubriko.creator.generated.resources.new_project_folder
import kubriko.creator.generated.resources.new_project_location
import kubriko.creator.generated.resources.new_project_name
import kubriko.creator.generated.resources.new_project_package
import kubriko.creator.generated.resources.new_project_title
import kubriko.creator.generated.resources.new_project_version_control
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.io.File

@Composable
internal fun NewProjectScreen(
    error: StringResource?,
    defaultParentDirectory: File,
    onCreate: (NewProjectRequest) -> Unit,
    onCancel: () -> Unit,
) {
    var form by remember { mutableStateOf(NewProjectForm(parentDirectory = defaultParentDirectory)) }
    val pickerTitle = stringResource(Res.string.choose_parent_folder)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.new_project_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            TemplateDescription(ProjectTemplate.STARTER)
            ValidatedTextField(
                label = Res.string.new_project_name,
                value = form.name,
                error = form.nameError,
                onValueChange = { form = form.withName(it) },
            )
            ValidatedTextField(
                label = Res.string.new_project_package,
                value = form.packageName,
                error = form.packageNameError,
                onValueChange = { form = form.withPackageName(it) },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ValidatedTextField(
                    modifier = Modifier.weight(1f),
                    label = Res.string.new_project_location,
                    value = form.parentDirectory.path,
                    error = null,
                    onValueChange = { form = form.copy(parentDirectory = File(it)) },
                )
                OutlinedButton(
                    onClick = { pickFolder(pickerTitle, form.parentDirectory)?.let { form = form.copy(parentDirectory = it) } },
                ) {
                    Text(stringResource(Res.string.action_browse))
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = form.isUnderVersionControl,
                    onCheckedChange = { form = form.copy(isUnderVersionControl = it) },
                )
                Text(
                    text = stringResource(Res.string.new_project_version_control),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            form.request?.let { request ->
                Text(
                    text = stringResource(Res.string.new_project_folder, request.directory.path),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (error != null) {
                Text(
                    text = stringResource(error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(Res.string.action_cancel))
                }
                Button(
                    enabled = form.request != null,
                    onClick = { form.request?.let(onCreate) },
                ) {
                    Text(stringResource(Res.string.action_create_project))
                }
            }
        }
    }
}

@Composable
private fun TemplateDescription(
    template: ProjectTemplate,
) = Column {
    Text(
        text = stringResource(template.title),
        style = MaterialTheme.typography.titleSmall,
    )
    Text(
        text = stringResource(template.description),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ValidatedTextField(
    label: StringResource,
    value: String,
    error: StringResource?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) = OutlinedTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    label = { Text(stringResource(label)) },
    isError = error != null,
    supportingText = error?.let { { Text(stringResource(it)) } },
    singleLine = true,
)
