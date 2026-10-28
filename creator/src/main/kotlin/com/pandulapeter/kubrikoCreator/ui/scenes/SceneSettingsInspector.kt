/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.scenes.SetSceneSettings
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ColorField
import com.pandulapeter.kubrikoCreator.ui.actorTypes.MenuButton
import com.pandulapeter.kubrikoCreator.ui.actorTypes.NumberField
import com.pandulapeter.kubrikoCreator.ui.actorTypes.audioFileLabel
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_height
import kubriko.creator.generated.resources.actor_type_width
import kubriko.creator.generated.resources.scene_settings_camera_x
import kubriko.creator.generated.resources.scene_settings_camera_y
import kubriko.creator.generated.resources.scene_settings_center_x
import kubriko.creator.generated.resources.scene_settings_center_y
import kubriko.creator.generated.resources.scene_settings_music
import kubriko.creator.generated.resources.scene_settings_music_none
import org.jetbrains.compose.resources.stringResource

/**
 * A Scene's settings: its bounds, background color, where the camera starts, and its music.
 *
 * @param music The music files the project has, by their path from `composeResources`.
 * @param onChange Called with the values to change, as `set_scene_settings` takes them (without the Scene).
 */
@Composable
internal fun SceneSettingsInspector(
    values: SceneSettingsState.Values,
    music: List<String>,
    onChange: (SetSceneSettings.Input.() -> SetSceneSettings.Input) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(modifier = Modifier.weight(1f), label = Res.string.scene_settings_center_x, value = values.x, onValueChange = { onChange { copy(x = it) } })
        NumberField(modifier = Modifier.weight(1f), label = Res.string.scene_settings_center_y, value = values.y, onValueChange = { onChange { copy(y = it) } })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(modifier = Modifier.weight(1f), label = Res.string.actor_type_width, value = values.width, onValueChange = { if (it > 0f) onChange { copy(width = it) } })
        NumberField(modifier = Modifier.weight(1f), label = Res.string.actor_type_height, value = values.height, onValueChange = { if (it > 0f) onChange { copy(height = it) } })
    }
    ColorField(
        color = values.backgroundColor,
        onColorChange = { onChange { copy(backgroundColor = it) } },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(modifier = Modifier.weight(1f), label = Res.string.scene_settings_camera_x, value = values.cameraX, onValueChange = { onChange { copy(cameraX = it) } })
        NumberField(modifier = Modifier.weight(1f), label = Res.string.scene_settings_camera_y, value = values.cameraY, onValueChange = { onChange { copy(cameraY = it) } })
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.scene_settings_music),
            style = MaterialTheme.typography.labelMedium,
        )
        val none = stringResource(Res.string.scene_settings_music_none)
        MenuButton(
            label = values.music.ifEmpty { null }?.let(::audioFileLabel) ?: none,
            options = listOf("") + music,
            optionText = { it.ifEmpty { null }?.let(::audioFileLabel) ?: none },
            onSelected = { path -> onChange { copy(music = path) } },
        )
    }
}
