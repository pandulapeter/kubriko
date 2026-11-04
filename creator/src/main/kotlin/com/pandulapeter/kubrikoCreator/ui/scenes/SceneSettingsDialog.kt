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

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.scenes.SetSceneSettings
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_done
import kubriko.creator.generated.resources.action_restore_scene_settings
import kubriko.creator.generated.resources.scene_settings_title
import org.jetbrains.compose.resources.stringResource

/**
 * A Scene's settings, in a dialog, each change made at once. Until the Scene has [settings], the defaults show, and the
 * first change adds them; restoring the defaults takes them away again.
 *
 * @param music The music files the project has, by their path from `composeResources`.
 * @param onChange Called with the values to change, as `set_scene_settings` takes them (without the Scene).
 * @param onRestoreDefaults Removes [settings] from the Scene.
 */
@Composable
internal fun SceneSettingsDialog(
    settings: SceneActorEntry?,
    music: List<String>,
    onChange: (SetSceneSettings.Input.() -> SetSceneSettings.Input) -> Unit,
    onRestoreDefaults: (SceneActorEntry) -> Unit,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = stringResource(Res.string.scene_settings_title),
    maxWidth = 440.dp,
    onDismiss = onDismiss,
    confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_done)) } },
    dismissButton = settings?.let { { TextButton(onClick = { onRestoreDefaults(it) }) { Text(stringResource(Res.string.action_restore_scene_settings)) } } },
) {
    SceneSettingsFields(
        values = settings?.let { SceneSettingsState.read(it.state) } ?: SceneSettingsState.Values(),
        music = music,
        onChange = onChange,
    )
}
