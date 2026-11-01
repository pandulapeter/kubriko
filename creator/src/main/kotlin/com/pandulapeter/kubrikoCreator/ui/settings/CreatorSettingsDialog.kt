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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pandulapeter.kubrikoCreator.settings.CreatorSettings
import com.pandulapeter.kubrikoCreator.settings.ThemeChoice
import com.pandulapeter.kubrikoCreator.ui.pickFolder
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_browse
import kubriko.creator.generated.resources.action_done
import kubriko.creator.generated.resources.action_use_home_folder
import kubriko.creator.generated.resources.choose_parent_folder
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.math.roundToInt

/**
 * Creator settings: this user's, on this computer, whether a project is open or not.
 */
@Composable
internal fun CreatorSettingsDialog(
    settings: CreatorSettings,
    homeFolder: File,
    onAiEnabledChange: (Boolean) -> Unit,
    onThemeChange: (ThemeChoice) -> Unit,
    onInterfaceScaleChange: (Float) -> Unit,
    onDefaultProjectFolderChange: (File?) -> Unit,
    onDismiss: () -> Unit,
) = AlertDialog(
    modifier = Modifier.widthIn(max = 560.dp).padding(24.dp),
    properties = DialogProperties(usePlatformDefaultWidth = false),
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.settings_title)) },
    text = {
        Column(
            modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AiSection(
                isAiEnabled = settings.isAiEnabled,
                onAiEnabledChange = onAiEnabledChange,
            )
            AppearanceSection(
                theme = settings.theme,
                interfaceScale = settings.interfaceScale,
                onThemeChange = onThemeChange,
                onInterfaceScaleChange = onInterfaceScaleChange,
            )
            EditorSection(
                defaultProjectFolder = settings.defaultProjectFolder?.let(::File) ?: homeFolder,
                isHomeFolder = settings.defaultProjectFolder == null,
                onDefaultProjectFolderChange = onDefaultProjectFolderChange,
            )
        }
    },
    confirmButton = {
        TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_done)) }
    },
)

@Composable
private fun AiSection(
    isAiEnabled: Boolean,
    onAiEnabledChange: (Boolean) -> Unit,
) = Section(Res.string.settings_section_ai) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(Res.string.settings_ai_enabled),
            style = MaterialTheme.typography.bodyMedium,
        )
        val label = stringResource(Res.string.settings_ai_enabled)
        Switch(
            modifier = Modifier.semantics { contentDescription = label },
            checked = isAiEnabled,
            onCheckedChange = onAiEnabledChange,
        )
    }
    Text(
        text = stringResource(Res.string.settings_ai_enabled_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AppearanceSection(
    theme: ThemeChoice,
    interfaceScale: Float,
    onThemeChange: (ThemeChoice) -> Unit,
    onInterfaceScaleChange: (Float) -> Unit,
) = Section(Res.string.settings_section_appearance) {
    Label(Res.string.settings_theme)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ThemeChoice.entries.forEach { choice ->
            FilterChip(
                selected = choice == theme,
                onClick = { onThemeChange(choice) },
                label = { Text(stringResource(choice.label)) },
            )
        }
    }
    Label(Res.string.settings_interface_scale)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CreatorSettings.interfaceScales.forEach { scale ->
            FilterChip(
                selected = scale == interfaceScale,
                onClick = { onInterfaceScaleChange(scale) },
                label = { Text(interfaceScaleLabel(scale)) },
            )
        }
    }
}

@Composable
private fun EditorSection(
    defaultProjectFolder: File,
    isHomeFolder: Boolean,
    onDefaultProjectFolderChange: (File?) -> Unit,
) = Section(Res.string.settings_section_editor) {
    Label(Res.string.settings_default_project_folder)
    Text(
        text = defaultProjectFolder.path,
        style = MaterialTheme.typography.bodyMedium,
    )
    val pickerTitle = stringResource(Res.string.choose_parent_folder)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { pickFolder(pickerTitle, defaultProjectFolder)?.let(onDefaultProjectFolderChange) }) {
            Text(stringResource(Res.string.action_browse))
        }
        if (!isHomeFolder) {
            TextButton(onClick = { onDefaultProjectFolderChange(null) }) {
                Text(stringResource(Res.string.action_use_home_folder))
            }
        }
    }
}

@Composable
private fun Section(
    title: StringResource,
    content: @Composable () -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    content()
}

@Composable
private fun Label(
    text: StringResource,
) = Text(
    text = stringResource(text),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

private val ThemeChoice.label
    get() = when (this) {
        ThemeChoice.SYSTEM -> Res.string.settings_theme_system
        ThemeChoice.LIGHT -> Res.string.settings_theme_light
        ThemeChoice.DARK -> Res.string.settings_theme_dark
    }

/**
 * An interface scale as the settings show it, a percentage of Material's own sizes.
 */
internal fun interfaceScaleLabel(scale: Float) = "${(scale * 100).roundToInt()}%"
