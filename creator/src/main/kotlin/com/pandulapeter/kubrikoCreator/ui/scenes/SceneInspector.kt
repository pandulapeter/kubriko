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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorValues
import com.pandulapeter.kubrikoCreator.commands.scenes.SetSceneSettings
import com.pandulapeter.kubrikoCreator.scenes.ActorBodyValues
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.scenes.SceneView
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_remove_actor
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.scene_inspector_hint
import kubriko.creator.generated.resources.scene_settings_title
import org.jetbrains.compose.resources.stringResource

/**
 * The column beside the Scene: the selected Actor's values, the Scene settings when they are selected, or with nothing
 * selected, how to place Actors and a way to the Scene settings.
 *
 * @param actorTypes The project's block-built Actor types, whose Properties and colors their Actors show.
 * @param music The project's music files, for the Scene settings.
 */
@Composable
internal fun SceneInspector(
    view: SceneView,
    sceneName: String,
    selectedIndex: Int?,
    actorTypes: List<ActorTypeDefinition>,
    music: List<String>,
    onSelect: (Int?) -> Unit,
    onCommand: (Invocation<*>) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()),
) {
    // Read so that the inspector follows the Actors created from the Scene's content.
    view.loadRevision.collectAsState().value
    val entry = selectedIndex?.let { view.entries.getOrNull(it) }
    val actor = selectedIndex?.let { view.actors.getOrNull(it) }
    when {
        selectedIndex == null || entry == null -> {
            Text(
                modifier = Modifier.padding(12.dp),
                text = stringResource(Res.string.scene_inspector_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                onClick = {
                    val settingsIndex = SceneSettingsState.indexIn(view.entries)
                    if (settingsIndex >= 0) {
                        onSelect(settingsIndex)
                    } else {
                        onSelect(view.entries.size)
                        onCommand(Invocation(SetSceneSettings, SetSceneSettings.Input(sceneName)))
                    }
                },
            ) {
                Text(stringResource(Res.string.scene_settings_title))
            }
        }
        view.isBackground(selectedIndex) -> {
            PanelHeader(icon = Res.drawable.ic_scene, title = stringResource(Res.string.scene_settings_title)) {
                IconActionButton(icon = Res.drawable.ic_delete, label = Res.string.action_remove_actor, onClick = { onRemove(selectedIndex) })
            }
            SceneSettingsInspector(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                values = SceneSettingsState.read(entry.state),
                music = music,
                onChange = { change -> onCommand(Invocation(SetSceneSettings, SetSceneSettings.Input(sceneName).change())) },
            )
        }
        else -> {
            PanelHeader(icon = Res.drawable.ic_actor_type, title = entry.typeId) {
                IconActionButton(icon = Res.drawable.ic_delete, label = Res.string.action_remove_actor, onClick = { onRemove(selectedIndex) })
            }
            if (actor != null) {
                ActorInspector(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    body = ActorBodyValues.of(actor.body),
                    state = entry.state,
                    definition = actorTypes.firstOrNull { it.name == entry.typeId },
                    onChange = { change -> onCommand(Invocation(SetActorValues, SetActorValues.Input(sceneName, selectedIndex).change())) },
                )
            }
        }
    }
}
