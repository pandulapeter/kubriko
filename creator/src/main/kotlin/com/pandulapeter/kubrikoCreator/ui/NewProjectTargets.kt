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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.NewProjectForm
import com.pandulapeter.kubrikoCreator.ui.settings.title
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.new_project_targets
import org.jetbrains.compose.resources.stringResource

/**
 * The platforms a new project is built for, the desktop always; one this computer can't build for ([canBuild]) can't be
 * chosen, and can be turned on later in Project settings.
 */
@Composable
internal fun NewProjectTargets(
    form: NewProjectForm,
    canBuild: (GameTarget) -> Boolean,
    onFormChange: (NewProjectForm) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(text = stringResource(Res.string.new_project_targets), style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameTarget.entries.forEach { target ->
            val isOn = target in form.shownTargets
            FilterChip(
                enabled = target != GameTarget.DESKTOP && (isOn || canBuild(target)),
                selected = isOn,
                onClick = { onFormChange(form.withTarget(target, !isOn)) },
                label = { Text(stringResource(target.title)) },
            )
        }
    }
}
