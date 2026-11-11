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
import kubriko.creator.generated.resources.settings_ai_follow_delay
import kubriko.creator.generated.resources.settings_ai_follow_delay_description
import kubriko.creator.generated.resources.settings_ai_follow_delay_none
import kubriko.creator.generated.resources.settings_ai_follow_delay_seconds
import kubriko.creator.generated.resources.settings_default_project_folder
import kubriko.creator.generated.resources.settings_follow_ai
import kubriko.creator.generated.resources.settings_follow_ai_description
import kubriko.creator.generated.resources.settings_interface_scale
import kubriko.creator.generated.resources.settings_section_ai
import kubriko.creator.generated.resources.settings_section_appearance
import kubriko.creator.generated.resources.settings_section_editor
import kubriko.creator.generated.resources.settings_theme
import kubriko.creator.generated.resources.settings_theme_dark
import kubriko.creator.generated.resources.settings_theme_light
import kubriko.creator.generated.resources.settings_theme_system
import kubriko.creator.generated.resources.settings_title
import kubriko.creator.generated.resources.settings_turn_review
import kubriko.creator.generated.resources.settings_turn_review_description
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.math.roundToInt

/**
 * Creator settings, opened on [initialPage] if one is given, with [aiPages] (the assistant's and the AI connection's)
 * after the AI page while AI features are on.
 */
@Composable
internal fun CreatorSettingsDialog(
    settings: CreatorSettings,
    homeFolder: File,
    onAiEnabledChange: (Boolean) -> Unit,
    onThemeChange: (ThemeChoice) -> Unit,
    onInterfaceScaleChange: (Float) -> Unit,
    onDefaultProjectFolderChange: (File?) -> Unit,
    onFollowingAiChange: (Boolean) -> Unit,
    onAiFollowDelayChange: (Int) -> Unit,
    onTurnReviewShownChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    initialPage: CreatorSettingsPage? = null,
    aiPages: List<DialogPage> = emptyList(),
) = MasterDetailDialog(
    title = stringResource(Res.string.settings_title),
    onDismiss = onDismiss,
    initialPageKey = initialPage?.key,
    pages = listOf(
        DialogPage(
            key = CreatorSettingsPage.APPEARANCE.key,
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
            key = CreatorSettingsPage.EDITOR.key,
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
            key = CreatorSettingsPage.AI.key,
            icon = Res.drawable.ic_ai,
            title = stringResource(Res.string.settings_section_ai),
        ) {
            AiPage(
                settings = settings,
                onAiEnabledChange = onAiEnabledChange,
                onFollowingAiChange = onFollowingAiChange,
                onAiFollowDelayChange = onAiFollowDelayChange,
                onTurnReviewShownChange = onTurnReviewShownChange,
            )
        },
    ) + aiPages.takeIf { settings.isAiEnabled }.orEmpty(),
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
private fun AiPage(
    settings: CreatorSettings,
    onAiEnabledChange: (Boolean) -> Unit,
    onFollowingAiChange: (Boolean) -> Unit,
    onAiFollowDelayChange: (Int) -> Unit,
    onTurnReviewShownChange: (Boolean) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    SettingsSwitch(
        label = stringResource(Res.string.settings_ai_enabled),
        description = stringResource(Res.string.settings_ai_enabled_description),
        checked = settings.isAiEnabled,
        onCheckedChange = onAiEnabledChange,
    )
    SettingsSwitch(
        label = stringResource(Res.string.settings_follow_ai),
        description = stringResource(Res.string.settings_follow_ai_description),
        checked = settings.isFollowingAi,
        onCheckedChange = onFollowingAiChange,
        isEnabled = settings.isAiEnabled,
    )
    SettingsRow(
        label = stringResource(Res.string.settings_ai_follow_delay),
        description = stringResource(Res.string.settings_ai_follow_delay_description),
    ) {
        SegmentedChoice(
            options = CreatorSettings.aiFollowDelays,
            selected = settings.aiFollowDelayInMillis,
            optionText = { aiFollowDelayLabel(it) },
            onSelected = onAiFollowDelayChange,
            isEnabled = settings.isAiEnabled && settings.isFollowingAi,
        )
    }
    SettingsSwitch(
        label = stringResource(Res.string.settings_turn_review),
        description = stringResource(Res.string.settings_turn_review_description),
        checked = settings.isTurnReviewShown,
        onCheckedChange = onTurnReviewShownChange,
        isEnabled = settings.isAiEnabled,
    )
}

@Composable
private fun aiFollowDelayLabel(delayInMillis: Int) = if (delayInMillis == 0) {
    stringResource(Res.string.settings_ai_follow_delay_none)
} else {
    stringResource(Res.string.settings_ai_follow_delay_seconds, (delayInMillis / 1_000f).toString().removeSuffix(".0"))
}

@Composable
private fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    optionText: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    isEnabled: Boolean = true,
) = SingleChoiceSegmentedButtonRow {
    options.forEachIndexed { index, option ->
        SegmentedButton(
            selected = option == selected,
            onClick = { onSelected(option) },
            enabled = isEnabled,
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
