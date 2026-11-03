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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.ProjectSettingsSnapshot
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.TargetState
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.DialogPage
import com.pandulapeter.kubrikoCreator.ui.components.MasterDetailDialog
import com.pandulapeter.kubrikoCreator.ui.text
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_review_upgrade
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_desktop
import kubriko.creator.generated.resources.ic_edit
import kubriko.creator.generated.resources.ic_play
import kubriko.creator.generated.resources.project_settings_game_description
import kubriko.creator.generated.resources.project_settings_identity_description
import kubriko.creator.generated.resources.project_settings_libraries_description
import kubriko.creator.generated.resources.project_settings_section_game
import kubriko.creator.generated.resources.project_settings_section_identity
import kubriko.creator.generated.resources.project_settings_section_libraries
import kubriko.creator.generated.resources.project_settings_section_targets
import kubriko.creator.generated.resources.project_settings_targets_description
import kubriko.creator.generated.resources.project_settings_title
import kubriko.creator.generated.resources.project_settings_unreadable
import org.jetbrains.compose.resources.stringResource

/**
 * Project settings: the game's, kept in the project where its build reads them.
 *
 * @param settings The project's settings, or null while they are being read, or if they couldn't be ([failure]).
 * @param failure Why the settings couldn't be read, if they couldn't.
 * @param targets The platforms the game is built for, or null if they can't be changed here.
 * @param onToggleTarget Asks to turn a target on or off, which is shown before it is made.
 * @param onReviewUpgrade Opens the template upgrade that makes the build read the settings, if there is one.
 */
@Composable
internal fun ProjectSettingsDialog(
    settings: ProjectSettingsSnapshot?,
    failure: Throwable? = null,
    targets: List<TargetState>? = null,
    onChange: (SetProjectSettings.Input) -> Unit,
    onToggleTarget: (GameTarget, Boolean) -> Unit = { _, _ -> },
    onReviewUpgrade: (() -> Unit)?,
    onDismiss: () -> Unit,
) = MasterDetailDialog(
    title = stringResource(Res.string.project_settings_title),
    onDismiss = onDismiss,
    pages = when {
        failure != null -> listOf(
            DialogPage(
                key = "identity",
                icon = Res.drawable.ic_edit,
                title = stringResource(Res.string.project_settings_section_identity),
            ) {
                Text(
                    text = stringResource(Res.string.project_settings_unreadable, failure.message ?: failure.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            },
        )
        settings != null -> projectSettingsPages(settings, targets, onChange, onToggleTarget, onReviewUpgrade)
        else -> emptyList()
    },
)

@Composable
private fun projectSettingsPages(
    settings: ProjectSettingsSnapshot,
    targets: List<TargetState>?,
    onChange: (SetProjectSettings.Input) -> Unit,
    onToggleTarget: (GameTarget, Boolean) -> Unit,
    onReviewUpgrade: (() -> Unit)?,
) = listOfNotNull(
    DialogPage(
        key = "identity",
        icon = Res.drawable.ic_edit,
        title = stringResource(Res.string.project_settings_section_identity),
        description = stringResource(Res.string.project_settings_identity_description),
    ) {
        when (val identity = settings.identity) {
            null -> UnavailableNotice(CommandProblem.PROJECT_SETTINGS_UNAVAILABLE, onReviewUpgrade)
            else -> IdentityFields(identity = identity, onChange = onChange)
        }
    },
    DialogPage(
        key = "game",
        icon = Res.drawable.ic_play,
        title = stringResource(Res.string.project_settings_section_game),
        description = stringResource(Res.string.project_settings_game_description),
    ) {
        when (val game = settings.game) {
            null -> UnavailableNotice(
                problem = if (settings.identity == null) CommandProblem.PROJECT_SETTINGS_UNAVAILABLE else CommandProblem.GAME_SETTINGS_UNAVAILABLE,
                onReviewUpgrade = onReviewUpgrade,
            )
            else -> GameSettingsPage(reading = game, scenes = settings.scenes, onChange = onChange)
        }
    },
    targets?.let {
        DialogPage(
            key = "targets",
            icon = Res.drawable.ic_desktop,
            title = stringResource(Res.string.project_settings_section_targets),
            description = stringResource(Res.string.project_settings_targets_description),
        ) {
            TargetsPage(targets = it, onToggle = onToggleTarget)
        }
    },
    DialogPage(
        key = "libraries",
        icon = Res.drawable.ic_code,
        title = stringResource(Res.string.project_settings_section_libraries),
        description = stringResource(Res.string.project_settings_libraries_description),
    ) {
        LibrariesPage(libraries = settings.libraries)
    },
)

@Composable
private fun IdentityFields(
    identity: GameIdentity,
    onChange: (SetProjectSettings.Input) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    IdentityField.entries.forEach { field ->
        IdentityTextField(
            field = field,
            identity = identity,
            onChange = onChange,
        )
    }
}

@Composable
private fun IdentityTextField(
    field: IdentityField,
    identity: GameIdentity,
    onChange: (SetProjectSettings.Input) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    val value = field.valueOf(identity)
    var text by remember(field) { mutableStateOf(value) }
    var isFocused by remember(field) { mutableStateOf(false) }
    LaunchedEffect(value, isFocused) {
        if (!isFocused) text = value
    }
    val change = field.changeOf(identity, text)
    val isChanged = text != value
    CompactTextField(
        modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused },
        value = text,
        onValueChange = { typed ->
            val line = typed.replace('\n', ' ')
            text = line
            (field.changeOf(identity, line) as? IdentityFieldChange.Valid)?.takeIf { line != value }?.let { onChange(it.input) }
        },
        label = stringResource(field.label),
        isError = isChanged && change is IdentityFieldChange.Invalid,
    )
    if (isChanged && change is IdentityFieldChange.Invalid) {
        Text(
            text = change.reason.text(change.arguments),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/**
 * Says that the build doesn't read some of the settings yet ([problem]), offering the template upgrade if there is one.
 */
@Composable
private fun UnavailableNotice(
    problem: CommandProblem,
    onReviewUpgrade: (() -> Unit)?,
) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(
        text = problem.text(),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (onReviewUpgrade != null) {
        OutlinedButton(onClick = onReviewUpgrade) {
            Text(stringResource(Res.string.action_review_upgrade))
        }
    }
}
