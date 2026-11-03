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
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.settings.CreatorSettings
import com.pandulapeter.kubrikoCreator.ui.components.DialogPage
import com.pandulapeter.kubrikoCreator.ui.components.MasterDetailDialog
import com.pandulapeter.kubrikoCreator.ui.pickFolder
import com.pandulapeter.kubrikoCreator.ui.theme.ThemeChoice
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_browse
import kubriko.creator.generated.resources.action_use_home_folder
import kubriko.creator.generated.resources.choose_parent_folder
import kubriko.creator.generated.resources.ic_ai
import kubriko.creator.generated.resources.ic_folder_open
import kubriko.creator.generated.resources.ic_palette
import kubriko.creator.generated.resources.settings_ai_enabled
import kubriko.creator.generated.resources.settings_ai_enabled_description
import kubriko.creator.generated.resources.settings_default_project_folder
import kubriko.creator.generated.resources.settings_interface_scale
import kubriko.creator.generated.resources.settings_section_ai
import kubriko.creator.generated.resources.settings_section_appearance
import kubriko.creator.generated.resources.settings_section_editor
import kubriko.creator.generated.resources.settings_theme
import kubriko.creator.generated.resources.settings_theme_dark
import kubriko.creator.generated.resources.settings_theme_light
import kubriko.creator.generated.resources.settings_theme_system
import kubriko.creator.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.math.roundToInt

@Composable
internal fun CreatorSettingsDialog(
    settings: CreatorSettings,
    homeFolder: File,
    onAiEnabledChange: (Boolean) -> Unit,
    onThemeChange: (ThemeChoice) -> Unit,
    onInterfaceScaleChange: (Float) -> Unit,
    onDefaultProjectFolderChange: (File?) -> Unit,
    onDismiss: () -> Unit,
) = MasterDetailDialog(
    title = stringResource(Res.string.settings_title),
    onDismiss = onDismiss,
    pages = listOf(
        DialogPage(
            key = "appearance",
            icon = Res.drawable.ic_palette,
            title = stringResource(Res.string.settings_section_appearance),
        ) {
            AppearancePage(
                theme = settings.theme,
                interfaceScale = settings.interfaceScale,
                onThemeChange = onThemeChange,
                onInterfaceScaleChange = onInterfaceScaleChange,
            )
        },
        DialogPage(
            key = "editor",
            icon = Res.drawable.ic_folder_open,
            title = stringResource(Res.string.settings_section_editor),
        ) {
            EditorPage(
                defaultProjectFolder = settings.defaultProjectFolder?.let(::File) ?: homeFolder,
                isHomeFolder = settings.defaultProjectFolder == null,
                onDefaultProjectFolderChange = onDefaultProjectFolderChange,
            )
        },
        DialogPage(
            key = "ai",
            icon = Res.drawable.ic_ai,
            title = stringResource(Res.string.settings_section_ai),
        ) {
            SettingsSwitch(
                label = stringResource(Res.string.settings_ai_enabled),
                description = stringResource(Res.string.settings_ai_enabled_description),
                checked = settings.isAiEnabled,
                onCheckedChange = onAiEnabledChange,
            )
        },
    ),
)

@Composable
private fun AppearancePage(
    theme: ThemeChoice,
    interfaceScale: Float,
    onThemeChange: (ThemeChoice) -> Unit,
    onInterfaceScaleChange: (Float) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    SettingsRow(label = stringResource(Res.string.settings_theme)) {
        SegmentedChoice(
            options = ThemeChoice.entries,
            selected = theme,
            optionText = { stringResource(it.label) },
            onSelected = onThemeChange,
        )
    }
    SettingsRow(label = stringResource(Res.string.settings_interface_scale)) {
        SegmentedChoice(
            options = CreatorSettings.interfaceScales,
            selected = interfaceScale,
            optionText = { interfaceScaleLabel(it) },
            onSelected = onInterfaceScaleChange,
        )
    }
}

@Composable
private fun EditorPage(
    defaultProjectFolder: File,
    isHomeFolder: Boolean,
    onDefaultProjectFolderChange: (File?) -> Unit,
) {
    val pickerTitle = stringResource(Res.string.choose_parent_folder)
    SettingsRow(
        label = stringResource(Res.string.settings_default_project_folder),
        description = defaultProjectFolder.path,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isHomeFolder) {
                TextButton(onClick = { onDefaultProjectFolderChange(null) }) {
                    Text(stringResource(Res.string.action_use_home_folder))
                }
            }
            OutlinedButton(onClick = { pickFolder(pickerTitle, defaultProjectFolder)?.let(onDefaultProjectFolderChange) }) {
                Text(stringResource(Res.string.action_browse))
            }
        }
    }
}

@Composable
private fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    optionText: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) = SingleChoiceSegmentedButtonRow {
    options.forEachIndexed { index, option ->
        SegmentedButton(
            selected = option == selected,
            onClick = { onSelected(option) },
            shape = SegmentedButtonDefaults.itemShape(index, options.size, MaterialTheme.shapes.small),
            icon = {},
            label = { Text(text = optionText(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
    }
}

private val ThemeChoice.label
    get() = when (this) {
        ThemeChoice.SYSTEM -> Res.string.settings_theme_system
        ThemeChoice.LIGHT -> Res.string.settings_theme_light
        ThemeChoice.DARK -> Res.string.settings_theme_dark
    }

internal fun interfaceScaleLabel(scale: Float) = "${(scale * 100).roundToInt()}%"
