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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.template.NewProjectForm
import com.pandulapeter.kubrikoCreator.template.ProjectTemplate
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.new_project_from_idea
import kubriko.creator.generated.resources.new_project_from_idea_description
import kubriko.creator.generated.resources.new_project_pitch
import org.jetbrains.compose.resources.stringResource

/**
 * What a new project starts from: one of the templates, or, while AI features are on, an idea ([NewProjectForm.pitch])
 * the writers' room designs a game from with the user, typed as a paragraph.
 */
@Composable
internal fun ProjectStartChoice(
    form: NewProjectForm,
    onFormChange: (NewProjectForm) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    val isAiEnabled = LocalIsAiEnabled.current
    val currentForm by rememberUpdatedState(form)
    val currentOnFormChange by rememberUpdatedState(onFormChange)
    // An idea chosen before AI features were turned off can't be seen anymore, so the template shown is created instead.
    LaunchedEffect(isAiEnabled) { if (!isAiEnabled && currentForm.isFromIdea) currentOnFormChange(currentForm.copy(pitch = null)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProjectTemplate.entries.forEach { template ->
            FilterChip(
                selected = !form.isFromIdea && form.template == template,
                onClick = { onFormChange(form.copy(template = template, pitch = null)) },
                label = { Text(stringResource(template.title)) },
            )
        }
        if (isAiEnabled) {
            FilterChip(
                selected = form.isFromIdea,
                onClick = { if (!form.isFromIdea) onFormChange(form.copy(pitch = "")) },
                label = { Text(stringResource(Res.string.new_project_from_idea)) },
            )
        }
    }
    val pitch = form.pitch
    if (pitch != null && isAiEnabled) {
        Text(text = stringResource(Res.string.new_project_from_idea_description), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            value = pitch,
            onValueChange = { onFormChange(form.copy(pitch = it)) },
            label = { Text(stringResource(Res.string.new_project_pitch)) },
        )
    } else {
        Text(text = stringResource(form.template.description), style = MaterialTheme.typography.bodyMedium)
    }
}
