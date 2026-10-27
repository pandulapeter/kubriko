/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.withProperty
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_parameter_by
import kubriko.creator.generated.resources.block_parameter_text
import kubriko.creator.generated.resources.block_parameter_value
import kubriko.creator.generated.resources.block_property_choose
import kubriko.creator.generated.resources.block_property_none
import org.jetbrains.compose.resources.stringResource

/**
 * The parameters of a Block that changes a Property: which of the Actor type's [properties] (of the kind the Block
 * needs) it changes, and the value.
 */
@Composable
internal fun PropertyBlockParameters(
    block: BlockDefinition,
    properties: List<PropertyDefinition>,
    onBlockChange: (BlockDefinition) -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val (propertyName, kind) = block.propertyReference ?: return@Row
    val choices = propertyChoices(properties, kind)
    if (choices.isEmpty()) {
        Text(
            modifier = Modifier.padding(vertical = 6.dp),
            text = stringResource(Res.string.block_property_none, stringResource(kind.title).lowercase()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return@Row
    }
    MenuButton(
        label = propertyName.ifEmpty { stringResource(Res.string.block_property_choose) },
        options = choices,
        optionText = { it },
        onSelected = { onBlockChange(block.withProperty(it)) },
    )
    when (block) {
        is BlockDefinition.SetNumber -> NumberField(
            modifier = Modifier.width(96.dp),
            label = Res.string.block_parameter_value,
            value = block.value,
            onValueChange = { onBlockChange(block.copy(value = it)) },
        )
        is BlockDefinition.ChangeNumber -> NumberField(
            modifier = Modifier.width(96.dp),
            label = Res.string.block_parameter_by,
            value = block.by,
            onValueChange = { onBlockChange(block.copy(by = it)) },
        )
        is BlockDefinition.SetText -> CompactTextField(
            modifier = Modifier.width(180.dp),
            value = block.value,
            onValueChange = { onBlockChange(block.copy(value = it)) },
            placeholder = stringResource(Res.string.block_parameter_text),
        )
        is BlockDefinition.SetToggle -> Switch(
            checked = block.value,
            onCheckedChange = { onBlockChange(block.copy(value = it)) },
        )
        else -> Unit
    }
}

/**
 * The names of the [properties] of [kind], which a Block that needs that kind can change.
 */
internal fun propertyChoices(properties: List<PropertyDefinition>, kind: PropertyKind) = properties.filter { PropertyKind.of(it) == kind }.map { it.name }
