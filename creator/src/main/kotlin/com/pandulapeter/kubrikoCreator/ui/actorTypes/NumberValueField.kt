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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.ui.components.MenuIconButton
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_number
import kubriko.creator.generated.resources.ic_property
import kubriko.creator.generated.resources.ic_random
import kubriko.creator.generated.resources.number_value_fixed
import kubriko.creator.generated.resources.number_value_from
import kubriko.creator.generated.resources.number_value_kind
import kubriko.creator.generated.resources.number_value_property
import kubriko.creator.generated.resources.number_value_random
import kubriko.creator.generated.resources.number_value_random_whole
import kubriko.creator.generated.resources.number_value_to
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A number a Block takes ([NumberValue]): a button showing its kind, which offers the others, then a number, the two a
 * random number is drawn between, or which of the number Properties in [scope] it reads.
 *
 * @param isWhole Whether the field only takes whole numbers.
 * @param accepts Which numbers the field takes; a number typed outside them isn't reported.
 */
@Composable
internal fun NumberValueField(
    label: StringResource,
    value: NumberValue,
    scope: BlockScope,
    onValueChange: (NumberValue) -> Unit,
    isWhole: Boolean = false,
    accepts: (Float) -> Boolean = { true },
    fieldWidth: Dp = 96.dp,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    fun isAccepted(number: Float) = accepts(number) && (!isWhole || number == number.toLong().toFloat())
    val kind = NumberValueKind.of(value, isWhole)
    MenuIconButton(
        icon = kind.icon,
        label = Res.string.number_value_kind,
        options = NumberValueKind.offeredBy(isWhole),
        optionText = { stringResource(it.title) },
        optionIcon = { it.icon },
        onSelected = { if (it != kind) onValueChange(it.of(value)) },
    )
    when (value) {
        is NumberValue.Fixed -> NumberField(
            modifier = Modifier.width(fieldWidth),
            label = label,
            value = value.value,
            onValueChange = { if (isAccepted(it)) onValueChange(NumberValue.Fixed(it)) },
        )
        is NumberValue.Random -> {
            FieldName(label)
            NumberField(
                modifier = Modifier.width(fieldWidth),
                label = Res.string.number_value_from,
                value = value.from,
                onValueChange = { if (isAccepted(it)) onValueChange(value.copy(from = it)) },
            )
            NumberField(
                modifier = Modifier.width(fieldWidth),
                label = Res.string.number_value_to,
                value = value.to,
                onValueChange = { if (isAccepted(it)) onValueChange(value.copy(to = it)) },
            )
        }
        is NumberValue.Property -> {
            FieldName(label)
            PropertyChoice(PropertyReference(value.property, PropertyKind.NUMBER, value.manager), scope) {
                onValueChange(NumberValue.Property(property = it.name, manager = it.manager))
            }
        }
    }
}

@Composable
private fun FieldName(label: StringResource) = Text(
    text = stringResource(label),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

private val NumberValueKind.icon
    get() = when (this) {
        NumberValueKind.FIXED -> Res.drawable.ic_number
        NumberValueKind.RANDOM, NumberValueKind.RANDOM_WHOLE -> Res.drawable.ic_random
        NumberValueKind.PROPERTY -> Res.drawable.ic_property
    }

private val NumberValueKind.title
    get() = when (this) {
        NumberValueKind.FIXED -> Res.string.number_value_fixed
        NumberValueKind.RANDOM -> Res.string.number_value_random
        NumberValueKind.RANDOM_WHOLE -> Res.string.number_value_random_whole
        NumberValueKind.PROPERTY -> Res.string.number_value_property
    }
