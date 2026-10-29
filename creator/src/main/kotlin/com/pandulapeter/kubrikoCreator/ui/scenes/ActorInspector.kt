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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.scenes.ActorValueChanges
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorValues
import com.pandulapeter.kubrikoCreator.scenes.ActorBodyValues
import com.pandulapeter.kubrikoCreator.scenes.ActorStateEdits
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ColorField
import com.pandulapeter.kubrikoCreator.ui.actorTypes.NumberField
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_height
import kubriko.creator.generated.resources.actor_type_width
import kubriko.creator.generated.resources.scene_inspector_rotation
import kubriko.creator.generated.resources.scene_inspector_scale_horizontal
import kubriko.creator.generated.resources.scene_inspector_scale_vertical
import kubriko.creator.generated.resources.scene_inspector_x
import kubriko.creator.generated.resources.scene_inspector_y
import org.jetbrains.compose.resources.stringResource

/**
 * The values of one Actor of a Scene: where it is, its size, rotation, and scale, and for a block-built Actor type, its
 * color and Property values (with the Actor type's for those it doesn't keep itself).
 *
 * @param state The Actor's saved state.
 * @param definition Its Actor type, if it is block-built.
 * @param onChange Called with the values to change, as `set_actor_values` takes them (without the Scene and the index).
 */
@Composable
internal fun ActorInspector(
    body: ActorBodyValues,
    state: String,
    definition: ActorTypeDefinition?,
    onChange: (SetActorValues.Input.() -> SetActorValues.Input) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    NumberPair(
        firstLabel = stringResource(Res.string.scene_inspector_x),
        first = body.x,
        onFirstChange = { onChange { copy(x = it) } },
        secondLabel = stringResource(Res.string.scene_inspector_y),
        second = body.y,
        onSecondChange = { onChange { copy(y = it) } },
    )
    body.size?.let { (width, height) ->
        NumberPair(
            firstLabel = stringResource(Res.string.actor_type_width),
            first = width,
            onFirstChange = { if (it > 0f) onChange { copy(width = it) } },
            secondLabel = stringResource(Res.string.actor_type_height),
            second = height,
            onSecondChange = { if (it > 0f) onChange { copy(height = it) } },
        )
        NumberField(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.scene_inspector_rotation),
            value = body.rotationInDegrees,
            onValueChange = { onChange { copy(rotation = it) } },
        )
        NumberPair(
            firstLabel = stringResource(Res.string.scene_inspector_scale_horizontal),
            first = body.scaleHorizontal,
            onFirstChange = { onChange { copy(scaleHorizontal = it) } },
            secondLabel = stringResource(Res.string.scene_inspector_scale_vertical),
            second = body.scaleVertical,
            onSecondChange = { onChange { copy(scaleVertical = it) } },
        )
    }
    if (definition != null) {
        if (definition.sprite == null) {
            ColorField(
                color = ActorStateEdits.colorOf(state) ?: definition.color,
                onColorChange = { onChange { copy(color = it) } },
            )
        }
        definition.properties.forEach { property ->
            PropertyValueField(
                property = property,
                state = state,
                onChange = { value -> onChange { copy(properties = listOf(value)) } },
            )
        }
    }
}

@Composable
private fun NumberPair(
    firstLabel: String,
    first: Float,
    onFirstChange: (Float) -> Unit,
    secondLabel: String,
    second: Float,
    onSecondChange: (Float) -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    NumberField(modifier = Modifier.weight(1f), label = firstLabel, value = first, onValueChange = onFirstChange)
    NumberField(modifier = Modifier.weight(1f), label = secondLabel, value = second, onValueChange = onSecondChange)
}

@Composable
private fun PropertyValueField(
    property: PropertyDefinition,
    state: String,
    onChange: (ActorValueChanges.PropertyValue) -> Unit,
) {
    val saved = ActorStateEdits.propertyOf(state, property.name)
    when (property) {
        is PropertyDefinition.Number -> NumberField(
            modifier = Modifier.fillMaxWidth(),
            label = property.name,
            value = saved?.floatOrNull ?: property.initialValue,
            onValueChange = { onChange(ActorValueChanges.PropertyValue(property.name, number = it)) },
        )
        is PropertyDefinition.Text -> CompactTextField(
            modifier = Modifier.fillMaxWidth(),
            value = saved?.takeIf { it.isString }?.content ?: property.initialValue,
            onValueChange = { onChange(ActorValueChanges.PropertyValue(property.name, text = it)) },
            label = property.name,
        )
        is PropertyDefinition.Toggle -> Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = property.name,
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = saved?.booleanOrNull ?: property.initialValue,
                onCheckedChange = { onChange(ActorValueChanges.PropertyValue(property.name, toggle = it)) },
            )
        }
    }
}
