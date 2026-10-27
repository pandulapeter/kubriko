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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_property_choose
import kubriko.creator.generated.resources.block_property_none
import org.jetbrains.compose.resources.stringResource

/**
 * Which of the Actor type's [properties] of [kind] a Block uses, or a hint to add one if there is none.
 */
@Composable
internal fun PropertyChoice(
    name: String,
    properties: List<PropertyDefinition>,
    kind: PropertyKind,
    onSelected: (String) -> Unit,
) {
    val choices = propertyChoices(properties, kind)
    if (choices.isEmpty()) {
        Text(
            modifier = Modifier.padding(vertical = 6.dp),
            text = stringResource(Res.string.block_property_none, stringResource(kind.title).lowercase()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        MenuButton(
            label = name.ifEmpty { stringResource(Res.string.block_property_choose) },
            options = choices,
            optionText = { it },
            onSelected = onSelected,
        )
    }
}

/**
 * The names of the [properties] of [kind], which a Block that needs that kind can use.
 */
internal fun propertyChoices(properties: List<PropertyDefinition>, kind: PropertyKind) = properties.filter { PropertyKind.of(it) == kind }.map { it.name }
