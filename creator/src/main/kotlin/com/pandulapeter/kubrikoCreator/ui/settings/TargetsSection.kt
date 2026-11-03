/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.TargetProblem
import com.pandulapeter.kubrikoCreator.template.TargetState
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.text
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.project_settings_section_targets
import kubriko.creator.generated.resources.project_settings_targets_description
import org.jetbrains.compose.resources.stringResource

/**
 * The platforms the game is built for, each turned on or off by [onToggle], which shows the change before making it.
 */
@Composable
internal fun TargetsSection(
    targets: List<TargetState>,
    onToggle: (GameTarget, Boolean) -> Unit,
) = SettingsSection(
    title = Res.string.project_settings_section_targets,
    description = Res.string.project_settings_targets_description,
) {
    targets.forEach { state ->
        TargetRow(
            state = state,
            onToggle = onToggle,
        )
    }
}

@Composable
private fun TargetRow(
    state: TargetState,
    onToggle: (GameTarget, Boolean) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    LabeledSwitch(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(state.target.title),
        checked = state.isOn,
        onCheckedChange = { onToggle(state.target, it) },
        labelStyle = MaterialTheme.typography.bodyMedium,
        enabled = state.canBeToggled,
    )
    state.problem?.let { problem ->
        Text(
            text = problem.text(),
            style = MaterialTheme.typography.bodySmall,
            color = if (problem == TargetProblem.ANDROID_SDK_MISSING_WHILE_ON) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Whether the switch of this target can be used: the desktop is always on, and a target this computer can't build can only
 * be turned off.
 */
internal val TargetState.canBeToggled get() = target != GameTarget.DESKTOP && (isOn || problem == null)
