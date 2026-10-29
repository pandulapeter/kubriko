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
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_property_choose
import kubriko.creator.generated.resources.block_property_none
import kubriko.creator.generated.resources.block_property_of_manager
import org.jetbrains.compose.resources.stringResource

/**
 * Which of the Properties in [scope] of the [reference]'s kind a Block uses: one of the type's own, or one of a Manager
 * type's, or a hint to add one if there is none.
 */
@Composable
internal fun PropertyChoice(
    reference: PropertyReference,
    scope: BlockScope,
    onSelected: (PropertyReference) -> Unit,
) {
    val choices = propertyChoices(scope, reference.kind)
    if (choices.isEmpty()) {
        Text(
            modifier = Modifier.padding(vertical = 6.dp),
            text = stringResource(Res.string.block_property_none, stringResource(reference.kind.title).lowercase()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        MenuButton(
            label = if (reference.name.isEmpty()) stringResource(Res.string.block_property_choose) else propertyChoiceText(reference),
            options = choices,
            optionText = { propertyChoiceText(it) },
            onSelected = onSelected,
        )
    }
}

@Composable
private fun propertyChoiceText(reference: PropertyReference) = reference.manager?.let { stringResource(Res.string.block_property_of_manager, reference.name, it) } ?: reference.name

/**
 * The Properties of [kind] a Block in [scope] can use: the type's own first, then those of each other Manager type.
 */
internal fun propertyChoices(scope: BlockScope, kind: PropertyKind) = scope.properties.filter { PropertyKind.of(it) == kind }.map { PropertyReference(it.name, kind) } +
    scope.managerTypes.filter { it.name != scope.typeName }.flatMap { manager ->
        manager.properties.filter { PropertyKind.of(it) == kind }.map { PropertyReference(it.name, kind, manager.name) }
    }
