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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.project.AspectRatio
import com.pandulapeter.kubrikoCreator.project.GameSettings
import com.pandulapeter.kubrikoCreator.project.GameSettingsProblem
import com.pandulapeter.kubrikoCreator.project.GameSettingsReading
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import com.pandulapeter.kubrikoCreator.ui.components.SearchableChoiceButton
import com.pandulapeter.kubrikoCreator.ui.text
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.project_settings_aspect_ratio
import kubriko.creator.generated.resources.project_settings_first_scene
import kubriko.creator.generated.resources.project_settings_frame_rate
import kubriko.creator.generated.resources.project_settings_frame_rate_display
import kubriko.creator.generated.resources.project_settings_frame_rate_limit
import kubriko.creator.generated.resources.project_settings_unreadable_keys
import kubriko.creator.generated.resources.project_settings_viewport_height
import kubriko.creator.generated.resources.project_settings_viewport_width
import org.jetbrains.compose.resources.stringResource

/**
 * The Game section of Project settings: the first Scene, the aspect ratio with the viewport's size, and the frame rate.
 *
 * @param scenes The project's Scenes, which the game can start with.
 */
@Composable
internal fun GameSettingsPage(
    reading: GameSettingsReading,
    scenes: List<String>,
    onChange: (SetProjectSettings.Input) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    val settings = reading.settings
    // A key the build can't read shows its default, which choosing has to write all the same.
    fun isChange(isSame: Boolean, key: String) = !isSame || key in reading.invalidKeys
    if (reading.invalidKeys.isNotEmpty()) {
        ProblemText(stringResource(Res.string.project_settings_unreadable_keys, reading.invalidKeys.joinToString()))
    }
    SettingsRow(label = stringResource(Res.string.project_settings_first_scene)) {
        SearchableChoiceButton(
            label = settings.firstScene,
            options = scenes,
            optionText = { it },
            onSelected = { if (isChange(it == settings.firstScene, GameSettings.FIRST_SCENE)) onChange(SetProjectSettings.Input(firstScene = it)) },
        )
    }
    if (settings.firstScene !in scenes) {
        ProblemText(CommandProblem.SCENE_MISSING.text(listOf(settings.firstScene)))
    }
    SettingsRow(
        label = stringResource(Res.string.project_settings_aspect_ratio),
        description = stringResource(settings.aspectRatio.description),
    ) {
        MenuButton(
            label = stringResource(settings.aspectRatio.label),
            options = AspectRatio.entries,
            optionText = { stringResource(it.label) },
            onSelected = { if (isChange(it == settings.aspectRatio, GameSettings.ASPECT_RATIO)) onChange(SetProjectSettings.Input(aspectRatio = it)) },
        )
    }
    if (settings.aspectRatio.usesWidth || settings.aspectRatio.usesHeight) {
        ViewportSizeFields(
            settings = settings,
            invalidKeys = reading.invalidKeys,
            onChange = onChange,
        )
    }
    SettingsRow(label = stringResource(Res.string.project_settings_frame_rate)) {
        MenuButton(
            label = frameRateText(settings.frameRate),
            options = frameRateOptions(settings.frameRate),
            optionText = { frameRateText(it) },
            onSelected = { if (isChange(it == settings.frameRate, GameSettings.FRAME_RATE)) onChange(SetProjectSettings.Input(frameRate = it)) },
        )
    }
}

/**
 * The width and height fields of the aspect ratios that use them; a size out of range isn't applied, and says why until
 * the fields lose the focus or the size changes elsewhere.
 */
@Composable
private fun ViewportSizeFields(
    settings: GameSettings,
    invalidKeys: List<String>,
    onChange: (SetProjectSettings.Input) -> Unit,
) {
    var problem by remember(settings) { mutableStateOf<Pair<GameSettingsProblem, List<String>>?>(null) }
    fun change(input: SetProjectSettings.Input) {
        problem = input.appliedTo(settings).validate()
        if (problem == null) onChange(input)
    }
    Row(
        modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.hasFocus) problem = null },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        if (settings.aspectRatio.usesWidth) {
            NumberField(
                modifier = Modifier.width(SIZE_FIELD_WIDTH),
                label = Res.string.project_settings_viewport_width,
                value = settings.viewportWidth,
                onValueChange = { if (it != settings.viewportWidth || GameSettings.VIEWPORT_WIDTH in invalidKeys) change(SetProjectSettings.Input(viewportWidth = it)) else problem = null },
            )
        }
        if (settings.aspectRatio.usesHeight) {
            NumberField(
                modifier = Modifier.width(SIZE_FIELD_WIDTH),
                label = Res.string.project_settings_viewport_height,
                value = settings.viewportHeight,
                onValueChange = { if (it != settings.viewportHeight || GameSettings.VIEWPORT_HEIGHT in invalidKeys) change(SetProjectSettings.Input(viewportHeight = it)) else problem = null },
            )
        }
    }
    problem?.let { (reason, arguments) -> ProblemText(reason.text(arguments)) }
}

@Composable
private fun frameRateText(frameRate: Int) = if (frameRate == GameSettings.EVERY_DISPLAY_FRAME) {
    stringResource(Res.string.project_settings_frame_rate_display)
} else {
    stringResource(Res.string.project_settings_frame_rate_limit, frameRate)
}

/**
 * A problem with the settings, in the error color.
 */
@Composable
private fun ProblemText(
    text: String,
) = Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.error,
)

private val SIZE_FIELD_WIDTH = 120.dp
