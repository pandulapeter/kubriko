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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_height
import kubriko.creator.generated.resources.actor_type_shape_circle
import kubriko.creator.generated.resources.actor_type_shape_rectangle
import kubriko.creator.generated.resources.actor_type_width
import org.jetbrains.compose.resources.stringResource

/**
 * How an Actor type looks, on one line: its shape, color, and the size it is placed with.
 */
@Composable
internal fun AppearanceSection(
    definition: ActorTypeDefinition,
    onChange: (ActorTypeDefinition) -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    ActorShape.entries.forEach { shape ->
        FilterChip(
            selected = definition.shape == shape,
            onClick = { onChange(definition.copy(shape = shape)) },
            label = { Text(stringResource(shape.title)) },
        )
    }
    ColorField(
        color = definition.color,
        onColorChange = { onChange(definition.copy(color = it)) },
    )
    NumberField(
        modifier = Modifier.width(110.dp),
        label = Res.string.actor_type_width,
        value = definition.width,
        onValueChange = { if (it > 0f) onChange(definition.copy(width = it)) },
    )
    NumberField(
        modifier = Modifier.width(110.dp),
        label = Res.string.actor_type_height,
        value = definition.height,
        onValueChange = { if (it > 0f) onChange(definition.copy(height = it)) },
    )
}

private val ActorShape.title
    get() = when (this) {
        ActorShape.CIRCLE -> Res.string.actor_type_shape_circle
        ActorShape.RECTANGLE -> Res.string.actor_type_shape_rectangle
    }
