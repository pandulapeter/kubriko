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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_appearance
import kubriko.creator.generated.resources.actor_type_collision_shape
import kubriko.creator.generated.resources.actor_type_height
import kubriko.creator.generated.resources.actor_type_shape_circle
import kubriko.creator.generated.resources.actor_type_shape_rectangle
import kubriko.creator.generated.resources.actor_type_width
import kubriko.creator.generated.resources.ic_sprite
import org.jetbrains.compose.resources.stringResource

/**
 * How an Actor type looks: its sprite, or its shape and color, and the size it is placed with. With a sprite, the
 * shape is only its collision mask's.
 *
 * @param sprites The images it can be drawn as, by their path from `composeResources`.
 */
@Composable
internal fun AppearanceColumn(
    definition: ActorTypeDefinition,
    sprites: List<String>,
    onChange: (SetAppearance.Input) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_sprite,
        title = stringResource(Res.string.actor_type_appearance),
    )
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SpriteChoice(
            sprite = definition.sprite,
            sprites = sprites,
            onSpriteChange = { onChange(SetAppearance.Input(definition.name, sprite = it)) },
        )
        if (definition.sprite != null) {
            Text(
                text = stringResource(Res.string.actor_type_collision_shape),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActorShape.entries.forEach { shape ->
                FilterChip(
                    selected = definition.shape == shape,
                    onClick = { onChange(SetAppearance.Input(definition.name, shape = shape)) },
                    label = { Text(stringResource(shape.title)) },
                )
            }
        }
        if (definition.sprite == null) {
            ColorField(
                color = definition.color,
                onColorChange = { onChange(SetAppearance.Input(definition.name, color = it)) },
            )
        }
        NumberField(
            modifier = Modifier.fillMaxWidth(),
            label = Res.string.actor_type_width,
            value = definition.width,
            onValueChange = { if (it > 0f) onChange(SetAppearance.Input(definition.name, width = it)) },
        )
        NumberField(
            modifier = Modifier.fillMaxWidth(),
            label = Res.string.actor_type_height,
            value = definition.height,
            onValueChange = { if (it > 0f) onChange(SetAppearance.Input(definition.name, height = it)) },
        )
    }
}

private val ActorShape.title
    get() = when (this) {
        ActorShape.CIRCLE -> Res.string.actor_type_shape_circle
        ActorShape.RECTANGLE -> Res.string.actor_type_shape_rectangle
    }
