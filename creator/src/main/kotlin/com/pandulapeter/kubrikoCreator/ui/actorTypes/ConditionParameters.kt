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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionKind
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_parameter_text
import kubriko.creator.generated.resources.block_parameter_value
import kubriko.creator.generated.resources.condition_axis_horizontal
import kubriko.creator.generated.resources.condition_axis_vertical
import kubriko.creator.generated.resources.condition_comparison_greater
import kubriko.creator.generated.resources.condition_comparison_greater_or_equal
import kubriko.creator.generated.resources.condition_comparison_less
import kubriko.creator.generated.resources.condition_comparison_less_or_equal
import kubriko.creator.generated.resources.condition_is
import kubriko.creator.generated.resources.condition_is_held
import kubriko.creator.generated.resources.condition_is_not
import kubriko.creator.generated.resources.condition_is_not_held
import kubriko.creator.generated.resources.condition_is_not_paused
import kubriko.creator.generated.resources.condition_is_off
import kubriko.creator.generated.resources.condition_is_on
import kubriko.creator.generated.resources.condition_is_paused
import kubriko.creator.generated.resources.condition_percent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What an if Block checks, read as a sentence: the kind of condition, then its parts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConditionParameters(
    condition: ConditionDefinition,
    scope: BlockScope,
    onConditionChange: (ConditionDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    MenuButton(
        label = stringResource(ConditionKind.of(condition).title),
        options = ConditionKind.entries.filter { it.isAvailableIn(scope.kind) },
        optionText = { stringResource(it.title) },
        onSelected = { if (it != ConditionKind.of(condition)) onConditionChange(it.createDefault()) },
    )
    when (condition) {
        is ConditionDefinition.Number -> {
            PropertyChoice(PropertyReference(condition.property, PropertyKind.NUMBER, condition.manager), scope) { onConditionChange(condition.copy(property = it.name, manager = it.manager)) }
            ComparisonChoice(condition.comparison) { onConditionChange(condition.copy(comparison = it)) }
            NumberField(
                modifier = Modifier.width(88.dp),
                label = Res.string.block_parameter_value,
                value = condition.value,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
            )
        }
        is ConditionDefinition.Text -> {
            PropertyChoice(PropertyReference(condition.property, PropertyKind.TEXT, condition.manager), scope) { onConditionChange(condition.copy(property = it.name, manager = it.manager)) }
            YesNoChoice(condition.isEqual, Res.string.condition_is, Res.string.condition_is_not) { onConditionChange(condition.copy(isEqual = it)) }
            CompactTextField(
                modifier = Modifier.width(140.dp),
                value = condition.value,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
                placeholder = stringResource(Res.string.block_parameter_text),
            )
        }
        is ConditionDefinition.Toggle -> {
            PropertyChoice(PropertyReference(condition.property, PropertyKind.TOGGLE, condition.manager), scope) { onConditionChange(condition.copy(property = it.name, manager = it.manager)) }
            YesNoChoice(condition.isOn, Res.string.condition_is_on, Res.string.condition_is_off) { onConditionChange(condition.copy(isOn = it)) }
        }
        is ConditionDefinition.KeyHeld -> {
            MenuButton(
                label = stringResource(condition.key.title),
                options = KeyChoice.entries,
                optionText = { stringResource(it.title) },
                onSelected = { onConditionChange(condition.copy(key = it)) },
            )
            YesNoChoice(condition.isHeld, Res.string.condition_is_held, Res.string.condition_is_not_held) { onConditionChange(condition.copy(isHeld = it)) }
        }
        is ConditionDefinition.Velocity -> {
            MenuButton(
                label = stringResource(condition.axis.title),
                options = Axis.entries,
                optionText = { stringResource(it.title) },
                onSelected = { onConditionChange(condition.copy(axis = it)) },
            )
            ComparisonChoice(condition.comparison) { onConditionChange(condition.copy(comparison = it)) }
            NumberField(
                modifier = Modifier.width(88.dp),
                label = Res.string.block_parameter_value,
                value = condition.value,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
            )
        }
        is ConditionDefinition.Chance -> NumberField(
            modifier = Modifier.width(88.dp),
            label = Res.string.condition_percent,
            value = condition.percent,
            onValueChange = { if (it in 0f..100f) onConditionChange(condition.copy(percent = it)) },
        )
        is ConditionDefinition.Paused -> YesNoChoice(condition.isPaused, Res.string.condition_is_paused, Res.string.condition_is_not_paused) { onConditionChange(condition.copy(isPaused = it)) }
    }
}

@Composable
private fun ComparisonChoice(
    comparison: Comparison,
    onSelected: (Comparison) -> Unit,
) = MenuButton(
    label = stringResource(comparison.title),
    options = Comparison.entries,
    optionText = { stringResource(it.title) },
    onSelected = onSelected,
)

@Composable
private fun YesNoChoice(
    value: Boolean,
    yes: StringResource,
    no: StringResource,
    onSelected: (Boolean) -> Unit,
) = MenuButton(
    label = stringResource(if (value) yes else no),
    options = listOf(true, false),
    optionText = { stringResource(if (it) yes else no) },
    onSelected = onSelected,
)

private val Comparison.title
    get() = when (this) {
        Comparison.LESS -> Res.string.condition_comparison_less
        Comparison.LESS_OR_EQUAL -> Res.string.condition_comparison_less_or_equal
        Comparison.EQUAL -> Res.string.condition_is
        Comparison.NOT_EQUAL -> Res.string.condition_is_not
        Comparison.GREATER_OR_EQUAL -> Res.string.condition_comparison_greater_or_equal
        Comparison.GREATER -> Res.string.condition_comparison_greater
    }

private val Axis.title
    get() = when (this) {
        Axis.HORIZONTAL -> Res.string.condition_axis_horizontal
        Axis.VERTICAL -> Res.string.condition_axis_vertical
    }
