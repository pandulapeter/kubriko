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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.TargetState
import com.pandulapeter.kubrikoCreator.ui.text
import kubriko.creator.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

/**
 * The platforms the game is built for, each turned on or off by [onToggle], which shows the change before making it.
 */
@Composable
internal fun TargetsPage(
    targets: List<TargetState>,
    onToggle: (GameTarget, Boolean) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    targets.forEach { state ->
        SettingsSwitch(
            label = stringResource(state.target.title),
            description = state.problem?.text(state.arguments),
            descriptionColor = if (state.isOn && state.target != GameTarget.DESKTOP) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            checked = state.isOn,
            onCheckedChange = { onToggle(state.target, it) },
            isEnabled = state.canBeToggled,
        )
    }
}

/**
 * Whether the switch of this target can be used: the desktop is always on, and a target this computer can't build can only
 * be turned off.
 */
internal val TargetState.canBeToggled get() = target != GameTarget.DESKTOP && (isOn || problem == null)
