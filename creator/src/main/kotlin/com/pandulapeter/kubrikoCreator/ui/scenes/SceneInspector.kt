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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.scenes.MoveActorOrder
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorValues
import com.pandulapeter.kubrikoCreator.commands.scenes.SetSceneValues
import com.pandulapeter.kubrikoCreator.commands.scenes.setActorStateValue
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorBodyValues
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneView
import com.pandulapeter.kubrikoCreator.scenes.ManagerSceneValues
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ReferenceChoices
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kotlinx.serialization.json.JsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_bring_actor_to_front
import kubriko.creator.generated.resources.action_duplicate_actor
import kubriko.creator.generated.resources.action_duplicate_actors
import kubriko.creator.generated.resources.action_remove_actor
import kubriko.creator.generated.resources.action_remove_actors
import kubriko.creator.generated.resources.action_send_actor_to_back
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_copy
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_manager_type
import kubriko.creator.generated.resources.ic_move_down
import kubriko.creator.generated.resources.ic_move_up
import kubriko.creator.generated.resources.scene_actor_not_created_hint
import kubriko.creator.generated.resources.scene_actors_selected
import kubriko.creator.generated.resources.scene_inspector_hint
import kubriko.creator.generated.resources.scene_settings_title
import kubriko.creator.generated.resources.scene_values_actor
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The column beside the Scene: the selected Actor's values, how many are selected while several are, or with nothing
 * selected, how to place Actors and a way to the Scene settings.
 *
 * @param actorTypes The project's block-built Actor types, whose Properties and colors their Actors show.
 * @param managerTypes The project's block-built Manager types, those with Properties of which the Scene can give values
 *   to (an entry of one without, left after its Properties were removed, can still be removed).
 * @param audioFiles The project's sound and music files, which sound and music Properties can name.
 */
@Composable
internal fun SceneInspector(
    view: SceneView,
    sceneName: String,
    selectedIds: List<String>,
    actorTypes: List<ActorTypeDefinition>,
    managerTypes: List<ManagerTypeDefinition>,
    audioFiles: AudioFiles?,
    onOpenSceneSettings: () -> Unit,
    onCommand: (Invocation<*>) -> Unit,
    onRemove: (List<String>) -> Unit,
    onDuplicate: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()),
) {
    // Read so that the inspector follows the Actors created from the Scene's content.
    view.loadRevision.collectAsState().value
    val selectedId = selectedIds.singleOrNull()
    val entry = selectedId?.let(view::entryOf)
    val actor = selectedId?.let(view::actorOf)
    val entries = view.entries
    val sceneValues = remember(entries, managerTypes) { ManagerSceneValues.valuesByManagerType(entries, managerTypes.mapTo(HashSet()) { it.name }) }
    val valueTakingManagerTypes = remember(managerTypes) { managerTypes.filter { it.properties.isNotEmpty() } }
    val selectedManager = entry?.let { selected -> managerTypes.firstOrNull { it.name == selected.typeId } }
    when {
        selectedIds.size > 1 -> PanelHeader(icon = Res.drawable.ic_actor_type, title = pluralStringResource(Res.plurals.scene_actors_selected, selectedIds.size, selectedIds.size), accent = Accent.CAST) {
            IconActionButton(icon = Res.drawable.ic_copy, label = Res.string.action_duplicate_actors, onClick = { onDuplicate(selectedIds) })
            IconActionButton(icon = Res.drawable.ic_delete, label = Res.string.action_remove_actors, onClick = { onRemove(selectedIds) })
        }
        selectedId == null || entry == null -> {
            Text(
                modifier = Modifier.padding(12.dp),
                text = stringResource(Res.string.scene_inspector_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                onClick = onOpenSceneSettings,
            ) {
                Text(stringResource(Res.string.scene_settings_title))
            }
            if (valueTakingManagerTypes.isNotEmpty()) SceneValues(valueTakingManagerTypes, sceneValues, sceneName, actorTypes, audioFiles, onCommand)
        }
        selectedManager != null -> {
            PanelHeader(icon = Res.drawable.ic_manager_type, title = stringResource(Res.string.scene_values_actor, selectedManager.name), accent = Accent.CREW) {
                IconActionButton(icon = Res.drawable.ic_delete, label = Res.string.action_remove_actor, onClick = { onRemove(selectedIds) })
            }
            SceneValues(listOf(selectedManager), sceneValues, sceneName, actorTypes, audioFiles, onCommand)
        }
        else -> {
            PanelHeader(icon = Res.drawable.ic_actor_type, title = entry.typeId, accent = Accent.CAST) {
                val index = view.entries.indexOf(entry)
                IconActionButton(
                    icon = Res.drawable.ic_move_down,
                    label = Res.string.action_send_actor_to_back,
                    onClick = { onCommand(Invocation(MoveActorOrder, MoveActorOrder.Input(sceneName, selectedId, 0))) },
                    isEnabled = index > 0,
                )
                IconActionButton(
                    icon = Res.drawable.ic_move_up,
                    label = Res.string.action_bring_actor_to_front,
                    onClick = { onCommand(Invocation(MoveActorOrder, MoveActorOrder.Input(sceneName, selectedId, view.entries.lastIndex))) },
                    isEnabled = index < view.entries.lastIndex,
                )
                IconActionButton(icon = Res.drawable.ic_copy, label = Res.string.action_duplicate_actor, onClick = { onDuplicate(selectedIds) })
                IconActionButton(icon = Res.drawable.ic_delete, label = Res.string.action_remove_actor, onClick = { onRemove(selectedIds) })
            }
            if (actor != null) {
                key(selectedId) {
                    ActorInspector(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        body = ActorBodyValues.of(actor.body),
                        state = entry.state,
                        definition = actorTypes.firstOrNull { it.name == entry.typeId },
                        choices = ReferenceChoices(actorTypes.map { it.name }, audioFiles),
                        onChange = { change -> onCommand(Invocation(SetActorValues, SetActorValues.Input(sceneName, selectedId).change())) },
                        onStateValueChange = { key, value, isTyped -> onCommand(setActorStateValue(sceneName, selectedId, key, value, isTyped)) },
                    )
                }
            } else {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = stringResource(Res.string.scene_actor_not_created_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SceneValues(
    managerTypes: List<ManagerTypeDefinition>,
    values: Map<String, Map<String, JsonPrimitive>>,
    sceneName: String,
    actorTypes: List<ActorTypeDefinition>,
    audioFiles: AudioFiles?,
    onCommand: (Invocation<*>) -> Unit,
) = ManagerSceneValuesSection(
    managerTypes = managerTypes,
    values = values,
    choices = ReferenceChoices(actorTypes.map { it.name }, audioFiles),
    onSet = { managerType, value -> onCommand(Invocation(SetSceneValues, SetSceneValues.Input(sceneName, managerType, values = listOf(value)))) },
    onClear = { managerType, property -> onCommand(Invocation(SetSceneValues, SetSceneValues.Input(sceneName, managerType, cleared = listOf(property)))) },
)
