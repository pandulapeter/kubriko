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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.scenes.ActorValueChanges
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kotlinx.serialization.json.JsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_clear_scene_value
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_manager_type
import kubriko.creator.generated.resources.scene_value_kept
import kubriko.creator.generated.resources.scene_values_hint
import org.jetbrains.compose.resources.stringResource

/**
 * What a Scene gives the Properties of [managerTypes] when it starts, one Manager type after the other: a value the Scene
 * sets can be changed or cleared, and one it doesn't set shows the Manager type's starting value, which changing sets it.
 *
 * @param values What the Scene sets, by Manager type and then by Property name.
 */
@Composable
internal fun ManagerSceneValuesSection(
    managerTypes: List<ManagerTypeDefinition>,
    values: Map<String, Map<String, JsonPrimitive>>,
    onSet: (managerType: String, ActorValueChanges.PropertyValue) -> Unit,
    onClear: (managerType: String, property: String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    Text(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        text = stringResource(Res.string.scene_values_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    managerTypes.forEach { managerType ->
        PanelHeader(icon = Res.drawable.ic_manager_type, title = managerType.name, accent = Accent.CREW)
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            managerType.properties.forEach { property ->
                SceneValueRow(
                    property = property,
                    value = values[managerType.name]?.get(property.name),
                    onSet = { onSet(managerType.name, it) },
                    onClear = { onClear(managerType.name, property.name) },
                )
            }
        }
    }
}

@Composable
private fun SceneValueRow(
    property: PropertyDefinition,
    value: JsonPrimitive?,
    onSet: (ActorValueChanges.PropertyValue) -> Unit,
    onClear: () -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    PropertyValueField(
        modifier = Modifier.weight(1f),
        property = property,
        value = value,
        label = if (value == null) stringResource(Res.string.scene_value_kept, property.name) else property.name,
        onChange = onSet,
    )
    if (value != null) {
        IconActionButton(icon = Res.drawable.ic_close, label = Res.string.action_clear_scene_value, onClick = onClear)
    }
}
